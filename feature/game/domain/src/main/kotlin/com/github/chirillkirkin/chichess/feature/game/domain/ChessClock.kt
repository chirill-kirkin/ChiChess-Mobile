package com.github.chirillkirkin.chichess.feature.game.domain

import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class TimeControl(val initial: Duration, val increment: Duration) {
  init {
    require(initial in InitialRange) { "Initial time $initial is outside $InitialRange" }
    require(increment in IncrementRange) { "Increment $increment is outside $IncrementRange" }
  }

  companion object {
    val InitialRange: ClosedRange<Duration> = 1.minutes..180.minutes
    val IncrementRange: ClosedRange<Duration> = Duration.ZERO..180.seconds
    val Default = TimeControl(initial = 10.minutes, increment = Duration.ZERO)
  }
}

/**
 * Remaining time of both sides. The time of [activeSide] is counted from [runningSince]; a clock with an
 * active side but no [runningSince] is paused.
 */
data class ChessClock(
  val timeControl: TimeControl,
  val whiteRemaining: Duration = timeControl.initial,
  val blackRemaining: Duration = timeControl.initial,
  // Null until each side has made its first move.
  val activeSide: PieceColor? = null,
  val runningSince: ComparableTimeMark? = null,
) {
  val isRunning: Boolean
    get() = runningSince != null

  fun remaining(color: PieceColor, now: ComparableTimeMark): Duration = remainingAfter(color) { now - it }

  fun remainingNow(color: PieceColor): Duration = remainingAfter(color, ComparableTimeMark::elapsedNow)

  fun afterMove(mover: PieceColor, now: ComparableTimeMark): ChessClock {
    val active = activeSide
    if (active == null) {
      // Black's first move starts White's clock.
      return if (mover == PieceColor.BLACK) copy(activeSide = PieceColor.WHITE, runningSince = now) else this
    }
    require(mover == active) { "$mover moved while the clock counts $active" }
    return withStoredRemaining(mover, remaining(mover, now) + timeControl.increment)
      .copy(activeSide = mover.opponent, runningSince = now)
  }

  fun paused(now: ComparableTimeMark): ChessClock {
    val active = activeSide
    if (active == null || !isRunning) return this
    return withStoredRemaining(active, remaining(active, now)).copy(runningSince = null)
  }

  fun resumed(now: ComparableTimeMark): ChessClock =
    if (activeSide == null || isRunning) this else copy(runningSince = now)

  fun deadline(): ComparableTimeMark? {
    val active = activeSide ?: return null
    return runningSince?.plus(storedRemaining(active))
  }

  fun expiredSide(now: ComparableTimeMark): PieceColor? =
    activeSide?.takeIf { isRunning && remaining(it, now) == Duration.ZERO }

  private inline fun remainingAfter(color: PieceColor, elapsedSince: (ComparableTimeMark) -> Duration): Duration {
    val since = runningSince?.takeIf { color == activeSide } ?: return storedRemaining(color)
    return (storedRemaining(color) - elapsedSince(since)).coerceAtLeast(Duration.ZERO)
  }

  private fun storedRemaining(color: PieceColor): Duration = when (color) {
    PieceColor.WHITE -> whiteRemaining
    PieceColor.BLACK -> blackRemaining
  }

  private fun withStoredRemaining(color: PieceColor, remaining: Duration): ChessClock = when (color) {
    PieceColor.WHITE -> copy(whiteRemaining = remaining)
    PieceColor.BLACK -> copy(blackRemaining = remaining)
  }

  companion object {
    // The clock counts once each side has made its first move.
    private const val UncountedHalfMoveCount = 2

    fun pausedOn(
      position: ChessPosition,
      timeControl: TimeControl,
      whiteRemaining: Duration,
      blackRemaining: Duration,
    ): ChessClock = ChessClock(
      timeControl = timeControl,
      whiteRemaining = whiteRemaining,
      blackRemaining = blackRemaining,
      activeSide = position.sideToMove.takeIf { position.moveHistory.size >= UncountedHalfMoveCount },
    )
  }
}
