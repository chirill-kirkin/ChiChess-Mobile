package com.github.chirillkirkin.chichess.feature.game.offline.data

import com.github.chirillkirkin.chichess.core.data.database.offline.OfflineGameDao
import com.github.chirillkirkin.chichess.core.data.database.offline.OfflineGameEntity
import com.github.chirillkirkin.chichess.core.data.database.offline.OfflineGameMoveEntity
import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.DrawReason
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.TimeControl
import com.github.chirillkirkin.chichess.feature.game.domain.toUci
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineBoardLayout
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineClock
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineGameRepository
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineGameSnapshot

class RoomOfflineGameRepository(private val dao: OfflineGameDao) : OfflineGameRepository {
  override suspend fun load(): OfflineGameSnapshot? = dao.getCurrentGame()?.toSnapshot()

  override suspend fun startNewGame(boardLayout: OfflineBoardLayout, timeControl: TimeControl?) {
    dao.replaceGame(
      OfflineGameEntity(
        drawReason = null,
        boardLayout = boardLayout.name,
        initialMs = timeControl?.initial?.inWholeMilliseconds,
        incrementMs = timeControl?.increment?.inWholeMilliseconds,
        whiteRemainingMs = timeControl?.initial?.inWholeMilliseconds,
        blackRemainingMs = timeControl?.initial?.inWholeMilliseconds,
      ),
    )
  }

  override suspend fun appendMove(halfMoveIndex: Int, move: ChessMove, clock: OfflineClock?) {
    val moveEntity = OfflineGameMoveEntity(halfMoveIndex = halfMoveIndex, uci = move.toUci())
    if (clock == null) {
      dao.insertMove(moveEntity)
    } else {
      dao.insertMoveWithClock(
        move = moveEntity,
        whiteRemainingMs = clock.whiteRemaining.inWholeMilliseconds,
        blackRemainingMs = clock.blackRemaining.inWholeMilliseconds,
      )
    }
  }

  override suspend fun updateBoardLayout(boardLayout: OfflineBoardLayout) {
    dao.updateBoardLayout(boardLayout.name)
  }

  override suspend fun updateDrawReason(drawReason: DrawReason) {
    dao.updateDrawReason(drawReason.name)
  }

  override suspend fun updateClock(clock: OfflineClock) {
    dao.updateClock(clock.whiteRemaining.inWholeMilliseconds, clock.blackRemaining.inWholeMilliseconds)
  }

  override suspend fun updateFlaggedSide(flaggedSide: PieceColor) {
    dao.updateFlaggedSide(flaggedSide.name)
  }
}
