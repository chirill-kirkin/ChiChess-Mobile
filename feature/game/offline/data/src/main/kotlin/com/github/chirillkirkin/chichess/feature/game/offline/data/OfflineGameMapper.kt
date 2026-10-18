package com.github.chirillkirkin.chichess.feature.game.offline.data

import com.github.chirillkirkin.chichess.core.data.database.offline.OfflineGameWithMoves
import com.github.chirillkirkin.chichess.feature.game.domain.DrawReason
import com.github.chirillkirkin.chichess.feature.game.domain.parseUci
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineBoardLayout
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineGameSnapshot

internal fun OfflineGameWithMoves.toSnapshot(): OfflineGameSnapshot = OfflineGameSnapshot(
  moves = moves
    .sortedBy { it.halfMoveIndex }
    .map { checkNotNull(parseUci(it.uci)) { "Cannot parse the stored move ${it.uci}" } },
  drawReason = game.drawReason?.let(DrawReason::valueOf),
  boardLayout = OfflineBoardLayout.valueOf(game.boardLayout),
)
