package com.github.chirillkirkin.chichess.feature.game.offline.data

import com.github.chirillkirkin.chichess.core.data.database.offline.OfflineGameEntity
import com.github.chirillkirkin.chichess.core.data.database.offline.OfflineGameWithMoves
import com.github.chirillkirkin.chichess.feature.game.domain.DrawReason
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.TimeControl
import com.github.chirillkirkin.chichess.feature.game.domain.parseUci
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineBoardLayout
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineClock
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineGameSnapshot
import kotlin.time.Duration.Companion.milliseconds

internal fun OfflineGameWithMoves.toSnapshot(): OfflineGameSnapshot = OfflineGameSnapshot(
  moves = moves
    .sortedBy { it.halfMoveIndex }
    .map { checkNotNull(parseUci(it.uci)) { "Cannot parse the stored move ${it.uci}" } },
  drawReason = game.drawReason?.let(DrawReason::valueOf),
  boardLayout = OfflineBoardLayout.valueOf(game.boardLayout),
  clock = game.toClock(),
  flaggedSide = game.flaggedSide?.let(PieceColor::valueOf),
)

private fun OfflineGameEntity.toClock(): OfflineClock? {
  val initialMs = initialMs ?: return null
  return OfflineClock(
    timeControl = TimeControl(initial = initialMs.milliseconds, increment = checkNotNull(incrementMs).milliseconds),
    whiteRemaining = checkNotNull(whiteRemainingMs).milliseconds,
    blackRemaining = checkNotNull(blackRemainingMs).milliseconds,
  )
}
