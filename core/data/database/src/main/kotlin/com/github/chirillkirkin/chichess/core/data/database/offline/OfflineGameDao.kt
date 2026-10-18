package com.github.chirillkirkin.chichess.core.data.database.offline

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert

@Dao
interface OfflineGameDao {
  @Transaction
  @Query("SELECT * FROM offline_game WHERE id = $CURRENT_OFFLINE_GAME_ID")
  suspend fun getCurrentGame(): OfflineGameWithMoves?

  @Upsert
  suspend fun upsertGame(game: OfflineGameEntity)

  @Insert
  suspend fun insertMove(move: OfflineGameMoveEntity)

  @Query("UPDATE offline_game SET board_layout = :boardLayout WHERE id = $CURRENT_OFFLINE_GAME_ID")
  suspend fun updateBoardLayout(boardLayout: String)

  @Query("UPDATE offline_game SET draw_reason = :drawReason WHERE id = $CURRENT_OFFLINE_GAME_ID")
  suspend fun updateDrawReason(drawReason: String?)

  @Query("DELETE FROM offline_game_move WHERE game_id = $CURRENT_OFFLINE_GAME_ID")
  suspend fun deleteMoves()

  @Transaction
  suspend fun replaceGame(game: OfflineGameEntity) {
    deleteMoves()
    upsertGame(game)
  }
}
