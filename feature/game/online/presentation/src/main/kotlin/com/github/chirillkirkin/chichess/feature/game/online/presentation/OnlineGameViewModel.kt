package com.github.chirillkirkin.chichess.feature.game.online.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.chirillkirkin.chichess.feature.game.domain.ChessGameEngine
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameChannel
import com.github.chirillkirkin.mvu.MVU
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = OnlineGameViewModel.Factory::class)
class OnlineGameViewModel @AssistedInject constructor(
  @Assisted gameId: String,
  channel: OnlineGameChannel,
  gameEngine: ChessGameEngine,
  savedStateHandle: SavedStateHandle,
) : ViewModel(),
  MVU<OnlineGameMessage, OnlineGameState, OnlineGameCommand> by savedStateHandle.onlineGameStore(
    gameId = gameId,
    connection = OnlineGameConnection(channel),
    gameEngine = gameEngine,
  ) {
  @AssistedFactory
  interface Factory {
    fun create(gameId: String): OnlineGameViewModel
  }

  init {
    launchIn(viewModelScope)
  }
}
