package com.github.chirillkirkin.chichess.core.data.database.offline

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey
import androidx.room3.Relation

// The table holds only the current offline game, stored under this fixed key.
const val CURRENT_OFFLINE_GAME_ID = 1L

@Entity(tableName = "offline_game")
data class OfflineGameEntity(
  @PrimaryKey val id: Long = CURRENT_OFFLINE_GAME_ID,
  @ColumnInfo(name = "draw_reason") val drawReason: String?,
  @ColumnInfo(name = "board_layout") val boardLayout: String,
)

@Entity(
  tableName = "offline_game_move",
  primaryKeys = ["game_id", "half_move_index"],
  foreignKeys = [
    ForeignKey(
      entity = OfflineGameEntity::class,
      parentColumns = ["id"],
      childColumns = ["game_id"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
)
data class OfflineGameMoveEntity(
  @ColumnInfo(name = "game_id") val gameId: Long = CURRENT_OFFLINE_GAME_ID,
  @ColumnInfo(name = "half_move_index") val halfMoveIndex: Int,
  val uci: String,
)

data class OfflineGameWithMoves(
  @Embedded val game: OfflineGameEntity,
  @Relation(parentColumns = ["id"], entityColumns = ["game_id"])
  val moves: List<OfflineGameMoveEntity>,
)
