package com.github.chirillkirkin.chichess

import com.github.chirillkirkin.chichess.feature.game.domain.ChessGameEngine
import com.github.chirillkirkin.chichess.feature.game.engine.ChesslibGameEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class GameEngineModule {
  @Binds
  abstract fun bindChessGameEngine(implementation: ChesslibGameEngine): ChessGameEngine
}
