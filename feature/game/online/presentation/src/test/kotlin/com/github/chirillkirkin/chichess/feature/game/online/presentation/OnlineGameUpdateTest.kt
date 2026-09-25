package com.github.chirillkirkin.chichess.feature.game.online.presentation

import com.github.chirillkirkin.chichess.feature.game.board.BoardMessage
import com.github.chirillkirkin.chichess.feature.game.domain.ChessFile
import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.ChessRank
import com.github.chirillkirkin.chichess.feature.game.domain.Fen
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.PieceType
import com.github.chirillkirkin.chichess.feature.game.domain.Square
import com.github.chirillkirkin.chichess.feature.game.engine.ChesslibGameEngine
import com.github.chirillkirkin.chichess.feature.game.online.domain.CommandRejection
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameEvent
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameResult
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameSnapshot
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameStatus
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineTerminationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val GAME_ID = "game-1"
private const val INVITE_CODE = "INV0000000"
private const val REVISION = 3L
private const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
private const val AFTER_E4_FEN = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
private const val FIRST_COMMAND_ID = "cmd-1"

private val e2 = Square(ChessFile.E, ChessRank.TWO)
private val e3 = Square(ChessFile.E, ChessRank.THREE)
private val e4 = Square(ChessFile.E, ChessRank.FOUR)

class OnlineGameUpdateTest {
  private val engine = ChesslibGameEngine()
  private var commandCounter = 0
  private val update = onlineGameUpdate(engine) { "cmd-${++commandCounter}" }

  @Test
  fun `snapshot connects and renders the board`() {
    val result = update(snapshot(), OnlineGameState(GAME_ID))

    val state = result.state
    assertEquals(ConnectionStatus.CONNECTED, state.connection)
    assertEquals(PieceColor.WHITE, state.yourColor)
    assertEquals(REVISION, state.revision)
    assertEquals(PieceColor.WHITE, state.board?.position?.sideToMove)
  }

  @Test
  fun `selecting a piece shows its legal targets`() {
    val connected = update(snapshot(), OnlineGameState(GAME_ID)).state

    val state = update(click(e2), connected).state

    assertEquals(e2, state.board?.selectedSquare)
    assertEquals(state.board?.legalTargets?.containsAll(setOf(e3, e4)), true)
  }

  @Test
  fun `a legal move is applied optimistically and sends the move`() {
    val afterSelect = update(click(e2), connected()).state

    val result = update(click(e4), afterSelect)

    val state = result.state
    assertEquals(ChessMove(e2, e4), state.pendingMove?.move)
    assertEquals(REVISION, state.pendingMove?.expectedRevision)
    assertNull(state.board?.position?.get(e2))
    assertEquals(PieceType.PAWN, state.board?.position?.get(e4)?.type)
    assertTrue(OnlineGameCommand.SendMove(FIRST_COMMAND_ID, REVISION, ChessMove(e2, e4)) in result.commands)
  }

  @Test
  fun `move applied confirms the pending move`() {
    val pending = movedOptimistically()

    val state = update(moveApplied(AFTER_E4_FEN, REVISION + 1), pending).state

    assertNull(state.pendingMove)
    assertEquals(REVISION + 1, state.revision)
    assertEquals(PieceColor.BLACK, state.board?.position?.sideToMove)
  }

  @Test
  fun `an illegal-move rejection rolls back and surfaces the error`() {
    val pending = movedOptimistically()

    val state = update(rejected(CommandRejection.ILLEGAL_MOVE), pending).state

    assertNull(state.pendingMove)
    assertEquals(CommandRejection.ILLEGAL_MOVE, state.moveError)
    assertEquals(PieceType.PAWN, state.board?.position?.get(e2)?.type)
    assertNull(state.board?.position?.get(e4))
  }

  @Test
  fun `a revision conflict rolls back without an error`() {
    val pending = movedOptimistically()

    val state = update(rejected(CommandRejection.REVISION_CONFLICT), pending).state

    assertNull(state.pendingMove)
    assertNull(state.moveError)
    assertEquals(PieceType.PAWN, state.board?.position?.get(e2)?.type)
  }

  @Test
  fun `draw offered then declined toggles the pending offer`() {
    val offered = update(OnlineGameMessage.Event(OnlineGameEvent.DrawOffered(PieceColor.BLACK)), connected()).state
    assertEquals(PieceColor.BLACK, offered.pendingDrawOfferBy)

    val declined = update(OnlineGameMessage.Event(OnlineGameEvent.DrawDeclined), offered).state
    assertNull(declined.pendingDrawOfferBy)
  }

  @Test
  fun `game finished sets the result`() {
    val event = OnlineGameMessage.Event(
      OnlineGameEvent.GameFinished(REVISION + 1, OnlineGameResult.WHITE_WON, OnlineTerminationReason.CHECKMATE),
    )

    val state = update(event, connected()).state

    assertEquals(OnlineGameStatus.FINISHED, state.status)
    assertEquals(OnlineGameResult.WHITE_WON, state.result)
    assertEquals(OnlineTerminationReason.CHECKMATE, state.terminationReason)
  }

  @Test
  fun `resign sends a resign command`() {
    val result = update(OnlineGameMessage.Resign, connected())

    assertTrue(OnlineGameCommand.SendResign(FIRST_COMMAND_ID, REVISION) in result.commands)
  }

  @Test
  fun `closed marks the connection closed`() {
    val state = update(OnlineGameMessage.Event(OnlineGameEvent.Closed(4403.toShort(), "NOT_A_GAME_PARTICIPANT")), connected()).state

    assertEquals(ConnectionStatus.CLOSED, state.connection)
  }

  @Test
  fun `clicks are ignored when it is not your turn`() {
    val connectedAsBlack = update(snapshot(PieceColor.BLACK), OnlineGameState(GAME_ID)).state

    val result = update(click(e2), connectedAsBlack)

    assertNull(result.state.board?.selectedSquare)
    assertTrue(result.commands.isEmpty())
  }

  @Test
  fun `clicks are ignored while a move is pending`() {
    val pending = movedOptimistically()

    val result = update(click(Square(ChessFile.D, ChessRank.SEVEN)), pending)

    assertEquals(pending, result.state)
    assertTrue(result.commands.isEmpty())
  }

  @Test
  fun `offer draw is sent while playing`() {
    val result = update(OnlineGameMessage.OfferDraw, connected())

    assertTrue(OnlineGameCommand.SendOfferDraw(FIRST_COMMAND_ID) in result.commands)
  }

  @Test
  fun `accept draw is sent only after the opponent offers`() {
    val withoutOffer = update(OnlineGameMessage.AcceptDraw, connected())
    assertTrue(withoutOffer.commands.isEmpty())

    val offered = update(OnlineGameMessage.Event(OnlineGameEvent.DrawOffered(PieceColor.BLACK)), connected()).state
    val accepted = update(OnlineGameMessage.AcceptDraw, offered)
    assertTrue(OnlineGameCommand.SendAcceptDraw(FIRST_COMMAND_ID) in accepted.commands)
  }

  private fun connected(): OnlineGameState = update(snapshot(), OnlineGameState(GAME_ID)).state

  private fun movedOptimistically(): OnlineGameState {
    val afterSelect = update(click(e2), connected()).state
    return update(click(e4), afterSelect).state
  }

  private fun click(square: Square) = OnlineGameMessage.Board(BoardMessage.SquareClick(square))

  private fun rejected(reason: CommandRejection) =
    OnlineGameMessage.Event(OnlineGameEvent.CommandRejected(FIRST_COMMAND_ID, reason))

  private fun moveApplied(fen: String, revision: Long) =
    OnlineGameMessage.Event(
      OnlineGameEvent.MoveApplied(
        revision = revision,
        fen = Fen(fen),
        status = OnlineGameStatus.IN_PROGRESS,
        lastMove = ChessMove(e2, e4),
        result = null,
        terminationReason = null,
      ),
    )

  private fun snapshot(color: PieceColor = PieceColor.WHITE) = OnlineGameMessage.Event(
    OnlineGameEvent.Snapshot(
      OnlineGameSnapshot(
        gameId = GAME_ID,
        inviteCode = INVITE_CODE,
        yourColor = color,
        status = OnlineGameStatus.IN_PROGRESS,
        revision = REVISION,
        fen = Fen(START_FEN),
      ),
    ),
  )
}
