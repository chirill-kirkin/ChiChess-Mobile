package com.github.chirillkirkin.chichess.feature.game.offline.data

import com.github.chirillkirkin.chichess.core.data.database.offline.OfflineGameDao
import com.github.chirillkirkin.chichess.core.data.database.offline.OfflineGameEntity
import com.github.chirillkirkin.chichess.core.data.database.offline.OfflineGameMoveEntity
import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.DrawReason
import com.github.chirillkirkin.chichess.feature.game.domain.toUci
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineBoardLayout
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineGameRepository
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineGameSnapshot

class RoomOfflineGameRepository(private val dao: OfflineGameDao) : OfflineGameRepository {
  override suspend fun load(): OfflineGameSnapshot? = dao.getCurrentGame()?.toSnapshot()

  override suspend fun startNewGame(boardLayout: OfflineBoardLayout) {
    dao.replaceGame(OfflineGameEntity(drawReason = null, boardLayout = boardLayout.name))
  }

  override suspend fun appendMove(halfMoveIndex: Int, move: ChessMove) {
    dao.insertMove(OfflineGameMoveEntity(halfMoveIndex = halfMoveIndex, uci = move.toUci()))
  }

  override suspend fun updateBoardLayout(boardLayout: OfflineBoardLayout) {
    dao.updateBoardLayout(boardLayout.name)
  }

  override suspend fun updateDrawReason(drawReason: DrawReason) {
    dao.updateDrawReason(drawReason.name)
  }
}
