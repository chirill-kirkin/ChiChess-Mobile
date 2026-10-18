package com.github.chirillkirkin.chichess.core.data.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.github.chirillkirkin.chichess.core.data.database.offline.OfflineGameDao
import com.github.chirillkirkin.chichess.core.data.database.offline.OfflineGameEntity
import com.github.chirillkirkin.chichess.core.data.database.offline.OfflineGameMoveEntity

const val CHICHESS_DATABASE_NAME = "chichess.db"

@Database(
  entities = [OfflineGameEntity::class, OfflineGameMoveEntity::class],
  version = 1,
)
abstract class ChiChessDatabase : RoomDatabase() {
  abstract fun offlineGameDao(): OfflineGameDao
}
