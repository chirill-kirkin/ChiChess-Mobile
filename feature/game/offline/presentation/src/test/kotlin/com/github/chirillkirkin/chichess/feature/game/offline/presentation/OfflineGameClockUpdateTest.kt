package com.github.chirillkirkin.chichess.feature.game.offline.presentation

import com.github.chirillkirkin.chichess.feature.game.board.BoardMessage
import com.github.chirillkirkin.chichess.feature.game.board.BoardState
import com.github.chirillkirkin.chichess.feature.game.domain.ChessClock
import com.github.chirillkirkin.chichess.feature.game.domain.ChessFile
import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.ChessPiece
import com.github.chirillkirkin.chichess.feature.game.domain.ChessPosition
import com.github.chirillkirkin.chichess.feature.game.domain.ChessRank
import com.github.chirillkirkin.chichess.feature.game.domain.DrawReason
import com.github.chirillkirkin.chichess.feature.game.domain.Fen
import com.github.chirillkirkin.chichess.feature.game.domain.GameStatus
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.PieceType
import com.github.chirillkirkin.chichess.feature.game.domain.Square
import com.github.chirillkirkin.chichess.feature.game.domain.TimeControl
import com.github.chirillkirkin.chichess.feature.game.engine.ChesslibGameEngine
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineBoardLayout
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineClock
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineGameSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class OfflineGameClockUpdateTest {
  private val timeSource = TestTimeSource()
  private val update = offlineGameUpdate(ChesslibGameEngine(), timeSource)
  private val timeControl = TimeControl(initial = 5.minutes, increment = 3.seconds)

  @Test
  fun `a new game with a time control starts with full clocks that do not run`() {
    val result = update(OfflineGameMessage.NewGame(timeControl), OfflineGameState())

    assertEquals(ChessClock(timeControl), result.state.clock)
    assertEquals(listOf(OfflineGameCommand.StartNewGame(OfflineBoardLayout.STANDARD, timeControl)), result.commands)
  }

  @Test
  fun `the clock starts after each side has moved and is stored with the move`() {
    val afterWhite = timedGame().reduceMove(E2, E4)
    val result = update(boardClick(E5), afterWhite.reduceBoardClick(E7))

    assertNull(afterWhite.clock?.activeSide)
    assertEquals(PieceColor.WHITE, result.state.clock?.activeSide)
    assertTrue(result.state.clock?.isRunning == true)
    assertEquals(
      listOf(OfflineGameCommand.AppendMove(1, ChessMove(E7, E5), OfflineClock(timeControl, 5.minutes, 5.minutes))),
      result.commands,
    )
  }

  @Test
  fun `a move charges the thinking time and adds the increment`() {
    val started = timedGame().reduceMove(E2, E4).reduceMove(E7, E5)
    timeSource += 10.seconds

    val moved = started.reduceMove(G1, F3)

    assertEquals(5.minutes - 7.seconds, moved.clock?.whiteRemaining)
    assertEquals(PieceColor.BLACK, moved.clock?.activeSide)
  }

  @Test
  fun `pausing stores the remaining time and blocks moves until resumed`() {
    val started = timedGame().reduceMove(E2, E4).reduceMove(E7, E5)
    timeSource += 10.seconds

    val pauseResult = update(OfflineGameMessage.PauseClock, started)
    val paused = pauseResult.state
    timeSource += 1.minutes
    val ignoredMove = paused.reduceMove(G1, F3)
    val resumed = paused.reduceMessage(OfflineGameMessage.ResumeClock)
    timeSource += 5.seconds

    assertTrue(paused.isClockPaused)
    assertEquals(
      listOf(OfflineGameCommand.UpdateClock(OfflineClock(timeControl, 5.minutes - 10.seconds, 5.minutes))),
      pauseResult.commands,
    )
    assertEquals(paused, ignoredMove)
    assertFalse(resumed.isClockPaused)
    assertEquals(5.minutes - 15.seconds, resumed.clock?.remaining(PieceColor.WHITE, timeSource.markNow()))
  }

  @Test
  fun `pausing a clock that has not started does nothing`() {
    val result = update(OfflineGameMessage.PauseClock, timedGame().reduceMove(E2, E4))

    assertFalse(result.state.isClockPaused)
    assertEquals(emptyList(), result.commands)
  }

  @Test
  fun `an expired clock loses the game and is stored`() {
    val started = timedGame().reduceMove(E2, E4).reduceMove(E7, E5)
    timeSource += 5.minutes

    val result = update(OfflineGameMessage.ClockExpired, started)

    assertEquals(GameStatus.Timeout(winner = PieceColor.BLACK), result.state.gameStatus)
    assertFalse(result.state.clock?.isRunning == true)
    assertEquals(
      listOf(
        OfflineGameCommand.UpdateClock(OfflineClock(timeControl, 0.seconds, 5.minutes)),
        OfflineGameCommand.UpdateFlaggedSide(PieceColor.WHITE),
      ),
      result.commands,
    )
  }

  @Test
  fun `a move after the deadline flags instead of moving`() {
    val started = timedGame().reduceMove(E2, E4).reduceMove(E7, E5)
    timeSource += 6.minutes

    val result = started.reduceMove(G1, F3)

    assertEquals(GameStatus.Timeout(winner = PieceColor.BLACK), result.gameStatus)
    assertEquals(started.board.position, result.board.position)
  }

  @Test
  fun `a flag against a lone king is a draw`() {
    val state =
      OfflineGameState(
        board =
          BoardState(
            position =
              ChessPosition.fromSnapshot(
                fen = Fen(LoneBlackKingFenValue),
                pieces =
                  mapOf(
                    Square(ChessFile.E, ChessRank.ONE) to ChessPiece(PieceColor.WHITE, PieceType.KING),
                    Square(ChessFile.A, ChessRank.TWO) to ChessPiece(PieceColor.WHITE, PieceType.PAWN),
                    Square(ChessFile.E, ChessRank.EIGHT) to ChessPiece(PieceColor.BLACK, PieceType.KING),
                  ),
                sideToMove = PieceColor.WHITE,
              ),
          ),
        clock = ChessClock(timeControl, activeSide = PieceColor.WHITE, runningSince = timeSource.markNow()),
      )
    timeSource += 5.minutes

    val flagged = state.reduceMessage(OfflineGameMessage.ClockExpired)

    assertEquals(GameStatus.Draw(DrawReason.TIMEOUT_VS_INSUFFICIENT_MATERIAL), flagged.gameStatus)
  }

  @Test
  fun `checkmate stops the clock`() {
    val finished =
      timedGame()
        .reduceMove(Square(ChessFile.F, ChessRank.TWO), Square(ChessFile.F, ChessRank.THREE))
        .reduceMove(E7, E5)
        .reduceMove(Square(ChessFile.G, ChessRank.TWO), Square(ChessFile.G, ChessRank.FOUR))
        .reduceMove(Square(ChessFile.D, ChessRank.EIGHT), Square(ChessFile.H, ChessRank.FOUR))

    assertEquals(GameStatus.Checkmate(PieceColor.BLACK), finished.gameStatus)
    assertFalse(finished.clock?.isRunning == true)
    assertNull(finished.clock?.deadline())
    assertFalse(finished.isClockPaused)
  }

  @Test
  fun `pausing drops a selected piece`() {
    val selected = timedGame().reduceMove(E2, E4).reduceMove(E7, E5).reduceBoardClick(G1)

    val paused = selected.reduceMessage(OfflineGameMessage.PauseClock)

    assertNull(paused.board.selectedSquare)
    assertEquals(emptySet(), paused.board.legalTargets)
  }

  @Test
  fun `pausing drops a pending promotion`() {
    val started = timedGame().reduceMove(E2, E4).reduceMove(E7, E5)
    val promoting = started.copy(pendingPromotion = ChessMove(E7, E5))

    val paused = promoting.reduceMessage(OfflineGameMessage.PauseClock)

    assertTrue(paused.isClockPaused)
    assertNull(paused.pendingPromotion)
  }

  @Test
  fun `a loaded game restores a paused clock and its flag`() {
    val snapshot =
      OfflineGameSnapshot(
        moves = listOf(ChessMove(E2, E4), ChessMove(E7, E5)),
        drawReason = null,
        boardLayout = OfflineBoardLayout.STANDARD,
        clock = OfflineClock(timeControl, whiteRemaining = 4.minutes, blackRemaining = 3.minutes),
      )

    val restored = update(OfflineGameMessage.GameLoaded(snapshot), OfflineGameState(isLoading = true)).state
    val flagged =
      update(
        OfflineGameMessage.GameLoaded(snapshot.copy(flaggedSide = PieceColor.WHITE)),
        OfflineGameState(isLoading = true),
      ).state

    assertTrue(restored.isClockPaused)
    assertEquals(PieceColor.WHITE, restored.clock?.activeSide)
    assertEquals(4.minutes, restored.clock?.whiteRemaining)
    assertEquals(GameStatus.Ongoing, restored.gameStatus)
    assertEquals(GameStatus.Timeout(winner = PieceColor.BLACK), flagged.gameStatus)
  }

  private fun timedGame(): OfflineGameState = OfflineGameState(clock = ChessClock(timeControl))

  private fun boardClick(square: Square): OfflineGameMessage =
    OfflineGameMessage.Board(BoardMessage.SquareClick(square))

  private fun OfflineGameState.reduceMessage(message: OfflineGameMessage): OfflineGameState =
    update(message, this).state

  private fun OfflineGameState.reduceBoardClick(square: Square): OfflineGameState = reduceMessage(boardClick(square))

  private fun OfflineGameState.reduceMove(from: Square, to: Square): OfflineGameState =
    reduceBoardClick(from).reduceBoardClick(to)

  private companion object {
    val E2 = Square(ChessFile.E, ChessRank.TWO)
    val E4 = Square(ChessFile.E, ChessRank.FOUR)
    val E7 = Square(ChessFile.E, ChessRank.SEVEN)
    val E5 = Square(ChessFile.E, ChessRank.FIVE)
    val G1 = Square(ChessFile.G, ChessRank.ONE)
    val F3 = Square(ChessFile.F, ChessRank.THREE)
    const val LoneBlackKingFenValue = "4k3/8/8/8/8/8/P7/4K3 w - - 0 1"
  }
}
