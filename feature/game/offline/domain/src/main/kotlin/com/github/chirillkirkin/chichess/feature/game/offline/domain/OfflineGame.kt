package com.github.chirillkirkin.chichess.feature.game.offline.domain

import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.DrawReason
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.TimeControl
import kotlin.time.Duration

enum class OfflineBoardLayout {
  STANDARD,
  BLACK_UPSIDE_DOWN,
  FLIP_AFTER_MOVE,
}

/** Remaining time of both sides as of the last stored move or pause. */
data class OfflineClock(val timeControl: TimeControl, val whiteRemaining: Duration, val blackRemaining: Duration)

data class OfflineGameSnapshot(
  val moves: List<ChessMove>,
  val drawReason: DrawReason?,
  val boardLayout: OfflineBoardLayout,
  val clock: OfflineClock? = null,
  val flaggedSide: PieceColor? = null,
)

interface OfflineGameRepository {
  suspend fun load(): OfflineGameSnapshot?

  suspend fun startNewGame(boardLayout: OfflineBoardLayout, timeControl: TimeControl?)

  suspend fun appendMove(halfMoveIndex: Int, move: ChessMove, clock: OfflineClock?)

  suspend fun updateBoardLayout(boardLayout: OfflineBoardLayout)

  suspend fun updateDrawReason(drawReason: DrawReason)

  suspend fun updateClock(clock: OfflineClock)

  suspend fun updateFlaggedSide(flaggedSide: PieceColor)
}
