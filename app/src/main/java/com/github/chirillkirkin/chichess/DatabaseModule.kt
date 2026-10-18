package com.github.chirillkirkin.chichess

import android.content.Context
import androidx.room3.Room
import com.github.chirillkirkin.chichess.core.data.database.CHICHESS_DATABASE_NAME
import com.github.chirillkirkin.chichess.core.data.database.ChiChessDatabase
import com.github.chirillkirkin.chichess.core.data.database.offline.OfflineGameDao
import com.github.chirillkirkin.chichess.feature.game.offline.data.RoomOfflineGameRepository
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineGameRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
  @Provides
  @Singleton
  fun provideChiChessDatabase(
    @ApplicationContext context: Context,
  ): ChiChessDatabase = Room.databaseBuilder<ChiChessDatabase>(context, CHICHESS_DATABASE_NAME).build()

  @Provides
  fun provideOfflineGameDao(database: ChiChessDatabase): OfflineGameDao = database.offlineGameDao()

  @Provides
  @Singleton
  fun provideOfflineGameRepository(dao: OfflineGameDao): OfflineGameRepository = RoomOfflineGameRepository(dao)
}
