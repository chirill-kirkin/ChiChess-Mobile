package com.github.chirillkirkin.chichess.feature.game.online.presentation.game

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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

private const val GAME_ID = "game-1"
private const val INVITE_CODE = "INV0000000"
private const val REVISION = 3L
private const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
private const val AFTER_E4_FEN = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
private const val FIRST_COMMAND_ID = "cmd-1"

private val EXPECTED_RECONNECT_DELAYS =
  listOf(1.seconds, 2.seconds, 4.seconds, 8.seconds, 16.seconds, 30.seconds, 30.seconds)

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
    val state = update(gameFinished(), connected()).state

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
  fun `a permanent close marks the connection closed without reconnecting`() {
    val result = update(closed(permanent = true), connected())

    assertEquals(ConnectionStatus.CLOSED, result.state.connection)
    assertTrue(result.commands.isEmpty())
  }

  @Test
  fun `a dropped connection rolls back the pending move and reconnects`() {
    val result = update(closed(permanent = false), movedOptimistically())

    assertEquals(ConnectionStatus.CONNECTING, result.state.connection)
    assertNull(result.state.pendingMove)
    assertEquals(PieceType.PAWN, result.state.board?.position?.get(e2)?.type)
    assertTrue(OnlineGameCommand.Connect(EXPECTED_RECONNECT_DELAYS.first()) in result.commands)
  }

  @Test
  fun `repeated drops back off up to the maximum delay`() {
    var state = connected()

    val delays = EXPECTED_RECONNECT_DELAYS.map {
      val result = update(closed(permanent = false), state)
      state = result.state
      result.commands.filterIsInstance<OnlineGameCommand.Connect>().single().delay
    }

    assertEquals(EXPECTED_RECONNECT_DELAYS, delays)
  }

  @Test
  fun `a snapshot after reconnecting resets the backoff`() {
    val reconnecting = update(closed(permanent = false), connected()).state
    val resynced = update(snapshot(), reconnecting).state

    val result = update(closed(permanent = false), resynced)

    assertTrue(OnlineGameCommand.Connect(EXPECTED_RECONNECT_DELAYS.first()) in result.commands)
  }

  @Test
  fun `a drop after the game finished does not reconnect`() {
    val finished = update(gameFinished(), connected()).state

    val result = update(closed(permanent = false), finished)

    assertEquals(ConnectionStatus.CLOSED, result.state.connection)
    assertTrue(result.commands.isEmpty())
  }

  @Test
  fun `returning to the foreground reconnects immediately`() {
    val result = update(OnlineGameMessage.AppForegrounded, connected())

    assertEquals(ConnectionStatus.CONNECTING, result.state.connection)
    assertTrue(OnlineGameCommand.Connect() in result.commands)
  }

  @Test
  fun `a permanently closed game does not reconnect in the foreground`() {
    val closed = update(closed(permanent = true), connected()).state

    val result = update(OnlineGameMessage.AppForegrounded, closed)

    assertTrue(result.commands.isEmpty())
  }

  @Test
  fun `going to the background disconnects`() {
    val result = update(OnlineGameMessage.AppBackgrounded, connected())

    assertEquals(listOf(OnlineGameCommand.Disconnect), result.commands.toList())
  }

  @Test
  fun `player joined requests a fresh sync`() {
    val result = update(OnlineGameMessage.Event(OnlineGameEvent.PlayerJoined(PieceColor.BLACK)), connected())

    assertTrue(OnlineGameCommand.RequestSync(FIRST_COMMAND_ID) in result.commands)
  }

  @Test
  fun `opponent leaving marks them disconnected without ending the game`() {
    val result = update(OnlineGameMessage.Event(OnlineGameEvent.PlayerLeft(PieceColor.BLACK)), connected())

    assertFalse(result.state.opponentConnected)
    assertTrue(result.commands.isEmpty())

    val afterSelect = update(click(e2), result.state).state
    val move = update(click(e4), afterSelect)
    assertTrue(OnlineGameCommand.SendMove(FIRST_COMMAND_ID, REVISION, ChessMove(e2, e4)) in move.commands)
  }

  @Test
  fun `opponent rejoining clears the disconnected state`() {
    val left = update(OnlineGameMessage.Event(OnlineGameEvent.PlayerLeft(PieceColor.BLACK)), connected()).state

    val result = update(OnlineGameMessage.Event(OnlineGameEvent.PlayerJoined(PieceColor.BLACK)), left)

    assertTrue(result.state.opponentConnected)
  }

  @Test
  fun `your own join does not mark an absent opponent connected`() {
    val opponentAbsent = update(snapshot(opponentConnected = false), OnlineGameState(GAME_ID)).state

    val result = update(OnlineGameMessage.Event(OnlineGameEvent.PlayerJoined(PieceColor.WHITE)), opponentAbsent)

    assertFalse(result.state.opponentConnected)
  }

  @Test
  fun `snapshot after reconnect clears the disconnected state`() {
    val left = update(OnlineGameMessage.Event(OnlineGameEvent.PlayerLeft(PieceColor.BLACK)), connected()).state

    val state = update(snapshot(opponentConnected = true), left).state

    assertTrue(state.opponentConnected)
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
  fun `offer draw is sent after the first move`() {
    val afterMove = update(moveApplied(AFTER_E4_FEN, REVISION + 1), connected()).state

    val result = update(OnlineGameMessage.OfferDraw, afterMove)

    assertTrue(OnlineGameCommand.SendOfferDraw(FIRST_COMMAND_ID) in result.commands)
  }

  @Test
  fun `offer draw is blocked before the first move`() {
    val result = update(OnlineGameMessage.OfferDraw, connected())

    assertTrue(result.commands.isEmpty())
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

  private fun closed(permanent: Boolean) = OnlineGameMessage.Event(OnlineGameEvent.Closed(permanent))

  private fun gameFinished() = OnlineGameMessage.Event(
    OnlineGameEvent.GameFinished(REVISION + 1, OnlineGameResult.WHITE_WON, OnlineTerminationReason.CHECKMATE),
  )

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

  private fun snapshot(
    color: PieceColor = PieceColor.WHITE,
    opponentConnected: Boolean = true,
  ) = OnlineGameMessage.Event(
    OnlineGameEvent.Snapshot(
      OnlineGameSnapshot(
        gameId = GAME_ID,
        inviteCode = INVITE_CODE,
        yourColor = color,
        status = OnlineGameStatus.IN_PROGRESS,
        revision = REVISION,
        fen = Fen(START_FEN),
      ),
      opponentConnected = opponentConnected,
    ),
  )
}
