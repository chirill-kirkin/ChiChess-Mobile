package com.github.chirillkirkin.chichess.feature.game.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class ChessClockTest {
  private val timeSource = TestTimeSource()
  private val clock = ChessClock(TimeControl(initial = 5.minutes, increment = 3.seconds))

  @Test
  fun `time control rejects values outside the allowed ranges`() {
    assertFailsWith<IllegalArgumentException> { TimeControl(initial = 30.seconds, increment = Duration.ZERO) }
    assertFailsWith<IllegalArgumentException> { TimeControl(initial = 181.minutes, increment = Duration.ZERO) }
    assertFailsWith<IllegalArgumentException> { TimeControl(initial = 5.minutes, increment = 181.seconds) }
  }

  @Test
  fun `the clock does not run before each side has moved`() {
    val afterWhite = clock.afterMove(PieceColor.WHITE, timeSource.markNow())
    timeSource += 20.seconds

    assertNull(afterWhite.activeSide)
    assertFalse(afterWhite.isRunning)
    assertEquals(5.minutes, afterWhite.remaining(PieceColor.BLACK, timeSource.markNow()))
  }

  @Test
  fun `black's first move starts white's clock without an increment`() {
    val started = clock
      .afterMove(
        PieceColor.WHITE,
        timeSource.markNow(),
      ).afterMove(PieceColor.BLACK, timeSource.markNow())
    timeSource += 10.seconds

    assertEquals(PieceColor.WHITE, started.activeSide)
    assertEquals(5.minutes - 10.seconds, started.remaining(PieceColor.WHITE, timeSource.markNow()))
    assertEquals(5.minutes, started.remaining(PieceColor.BLACK, timeSource.markNow()))
  }

  @Test
  fun `a move charges the elapsed time, adds the increment, and switches sides`() {
    val started = startedClock()
    timeSource += 10.seconds

    val switched = started.afterMove(PieceColor.WHITE, timeSource.markNow())
    timeSource += 4.seconds

    assertEquals(PieceColor.BLACK, switched.activeSide)
    assertEquals(5.minutes - 7.seconds, switched.remaining(PieceColor.WHITE, timeSource.markNow()))
    assertEquals(5.minutes - 4.seconds, switched.remaining(PieceColor.BLACK, timeSource.markNow()))
  }

  @Test
  fun `a move by the side that is not counted is rejected`() {
    assertFailsWith<IllegalArgumentException> { startedClock().afterMove(PieceColor.BLACK, timeSource.markNow()) }
  }

  @Test
  fun `a paused clock keeps its remaining time until resumed`() {
    val started = startedClock()
    timeSource += 10.seconds
    val paused = started.paused(timeSource.markNow())
    timeSource += 1.minutes

    assertFalse(paused.isRunning)
    assertEquals(PieceColor.WHITE, paused.activeSide)
    assertEquals(5.minutes - 10.seconds, paused.remaining(PieceColor.WHITE, timeSource.markNow()))
    assertNull(paused.deadline())

    val resumed = paused.resumed(timeSource.markNow())
    timeSource += 5.seconds

    assertTrue(resumed.isRunning)
    assertEquals(5.minutes - 15.seconds, resumed.remaining(PieceColor.WHITE, timeSource.markNow()))
  }

  @Test
  fun `a clock that has not started cannot be resumed`() {
    assertEquals(clock, clock.resumed(timeSource.markNow()))
  }

  @Test
  fun `the deadline is when the active side runs out of time`() {
    val started = startedClock()
    timeSource += 5.minutes - 1.seconds
    assertNull(started.expiredSide(timeSource.markNow()))

    timeSource += 1.seconds
    assertEquals(started.deadline(), timeSource.markNow())
    assertEquals(PieceColor.WHITE, started.expiredSide(timeSource.markNow()))
  }

  @Test
  fun `remaining time does not go below zero`() {
    val started = startedClock()
    timeSource += 10.minutes

    assertEquals(Duration.ZERO, started.remaining(PieceColor.WHITE, timeSource.markNow()))
  }

  private fun startedClock(): ChessClock =
    clock.afterMove(PieceColor.WHITE, timeSource.markNow()).afterMove(PieceColor.BLACK, timeSource.markNow())
}
