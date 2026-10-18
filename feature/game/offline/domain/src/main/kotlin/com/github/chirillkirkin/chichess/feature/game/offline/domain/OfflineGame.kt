package com.github.chirillkirkin.chichess.feature.game.offline.domain

import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.DrawReason

enum class OfflineBoardLayout {
  STANDARD,
  BLACK_UPSIDE_DOWN,
  FLIP_AFTER_MOVE,
}

data class OfflineGameSnapshot(
  val moves: List<ChessMove>,
  val drawReason: DrawReason?,
  val boardLayout: OfflineBoardLayout,
)

interface OfflineGameRepository {
  suspend fun load(): OfflineGameSnapshot?

  suspend fun startNewGame(boardLayout: OfflineBoardLayout)

  suspend fun appendMove(halfMoveIndex: Int, move: ChessMove)

  suspend fun updateBoardLayout(boardLayout: OfflineBoardLayout)

  suspend fun updateDrawReason(drawReason: DrawReason)
}
