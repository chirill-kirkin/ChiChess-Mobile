package com.github.chirillkirkin.chichess.feature.game.online.presentation.game

import com.github.chirillkirkin.chichess.feature.game.board.BoardMessage
import com.github.chirillkirkin.chichess.feature.game.domain.ChessFile
import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.ChessRank
import com.github.chirillkirkin.chichess.feature.game.domain.Fen
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.Square
import com.github.chirillkirkin.chichess.feature.game.domain.TimeControl
import com.github.chirillkirkin.chichess.feature.game.engine.ChesslibGameEngine
import com.github.chirillkirkin.chichess.feature.game.online.domain.ClockReading
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

private const val GAME_ID = "game-1"
private const val INVITE_CODE = "INV0000000"
private const val REVISION = 4L
private const val AFTER_E4_E5_FEN = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2"
private const val AFTER_NF3_FEN = "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2"

private val g1 = Square(ChessFile.G, ChessRank.ONE)
private val f3 = Square(ChessFile.F, ChessRank.THREE)

class OnlineGameClockUpdateTest {
  private val timeSource = TestTimeSource()
  private var commandCounter = 0
  private val update = onlineGameUpdate(ChesslibGameEngine(), timeSource) { "cmd-${++commandCounter}" }
  private val timeControl = TimeControl(initial = 5.minutes, increment = 3.seconds)

  @Test
  fun `a timed snapshot counts the running side from its arrival`() {
    val state = connected(ClockReading(290.seconds, 300.seconds, runningSide = PieceColor.WHITE))
    timeSource += 10.seconds

    assertEquals(timeControl, state.timeControl)
    assertEquals(280.seconds, state.clock?.remaining(PieceColor.WHITE, timeSource.markNow()))
    assertEquals(300.seconds, state.clock?.remaining(PieceColor.BLACK, timeSource.markNow()))
    assertEquals(state.clock, state.confirmedClock)
  }

  @Test
  fun `a snapshot without a time control has no clock`() {
    val state = update(snapshot(timeControl = null, clock = null), OnlineGameState(GAME_ID)).state

    assertNull(state.timeControl)
    assertNull(state.clock)
  }

  @Test
  fun `an optimistic move switches the shown clock and keeps the confirmed one`() {
    val connected = connected(ClockReading(290.seconds, 300.seconds, runningSide = PieceColor.WHITE))
    timeSource += 10.seconds

    val moved = connected.reduceMove(g1, f3)

    assertEquals(PieceColor.BLACK, moved.clock?.activeSide)
    assertEquals(283.seconds, moved.clock?.whiteRemaining)
    assertEquals(connected.confirmedClock, moved.confirmedClock)
  }

  @Test
  fun `a rejected move restores the confirmed clock`() {
    val connected = connected(ClockReading(290.seconds, 300.seconds, runningSide = PieceColor.WHITE))
    val moved = connected.reduceMove(g1, f3)

    val rejected =
      update(OnlineGameMessage.Event(OnlineGameEvent.CommandRejected("cmd-1", CommandRejection.ILLEGAL_MOVE)), moved)
        .state

    assertEquals(connected.confirmedClock, rejected.clock)
  }

  @Test
  fun `a confirmed move replaces the clock with the server reading`() {
    val moved = connected(ClockReading(290.seconds, 300.seconds, runningSide = PieceColor.WHITE)).reduceMove(g1, f3)
    val reading = ClockReading(285.seconds, 300.seconds, runningSide = PieceColor.BLACK)

    val confirmed =
      update(
        OnlineGameMessage.Event(
          OnlineGameEvent.MoveApplied(
            revision = REVISION + 1,
            fen = Fen(AFTER_NF3_FEN),
            status = OnlineGameStatus.IN_PROGRESS,
            lastMove = ChessMove(g1, f3),
            result = null,
            terminationReason = null,
            clock = reading,
          ),
        ),
        moved,
      ).state

    assertEquals(285.seconds, confirmed.clock?.whiteRemaining)
    assertEquals(PieceColor.BLACK, confirmed.clock?.activeSide)
    assertEquals(confirmed.clock, confirmed.confirmedClock)
  }

  @Test
  fun `a finished game stops the clock`() {
    val connected = connected(ClockReading(1.seconds, 300.seconds, runningSide = PieceColor.WHITE))

    val finished =
      update(
        OnlineGameMessage.Event(
          OnlineGameEvent.GameFinished(
            revision = REVISION + 1,
            result = OnlineGameResult.BLACK_WON,
            terminationReason = OnlineTerminationReason.TIMEOUT,
            clock = ClockReading(Duration.ZERO, 300.seconds, runningSide = null),
          ),
        ),
        connected,
      ).state

    assertFalse(finished.clock?.isRunning == true)
    assertEquals(Duration.ZERO, finished.clock?.whiteRemaining)
  }

  @Test
  fun `an expired clock without a flag from the server requests a sync`() {
    val connected = connected(ClockReading(5.seconds, 300.seconds, runningSide = PieceColor.WHITE))

    val early = update(OnlineGameMessage.ClockExpired, connected)
    timeSource += 6.seconds
    val late = update(OnlineGameMessage.ClockExpired, connected)

    assertTrue(early.commands.isEmpty())
    assertEquals(listOf<OnlineGameCommand>(OnlineGameCommand.RequestSync("cmd-1")), late.commands)
  }

  private fun connected(clock: ClockReading): OnlineGameState =
    update(snapshot(timeControl, clock), OnlineGameState(GAME_ID)).state

  private fun OnlineGameState.reduceMove(from: Square, to: Square): OnlineGameState {
    val selected = update(OnlineGameMessage.Board(BoardMessage.SquareClick(from)), this).state
    return update(OnlineGameMessage.Board(BoardMessage.SquareClick(to)), selected).state
  }

  private fun snapshot(timeControl: TimeControl?, clock: ClockReading?) = OnlineGameMessage.Event(
    OnlineGameEvent.Snapshot(
      OnlineGameSnapshot(
        gameId = GAME_ID,
        inviteCode = INVITE_CODE,
        yourColor = PieceColor.WHITE,
        status = OnlineGameStatus.IN_PROGRESS,
        revision = REVISION,
        fen = Fen(AFTER_E4_E5_FEN),
        timeControl = timeControl,
        clock = clock,
      ),
      opponentConnected = true,
    ),
  )
}
