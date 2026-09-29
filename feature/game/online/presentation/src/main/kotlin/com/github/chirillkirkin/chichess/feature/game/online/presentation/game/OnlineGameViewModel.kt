package com.github.chirillkirkin.chichess.feature.game.online.presentation.game

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
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
import kotlinx.coroutines.flow.map

@HiltViewModel(assistedFactory = OnlineGameViewModel.Factory::class)
class OnlineGameViewModel @AssistedInject constructor(
  @Assisted gameId: String,
  channel: OnlineGameChannel,
  gameEngine: ChessGameEngine,
  savedStateHandle: SavedStateHandle,
) : ViewModel(),
  MVU<OnlineGameMessage, OnlineGameState, OnlineGameCommand> by savedStateHandle.onlineGameStore(
    gameId = gameId,
    connection = OnlineGameConnection(channel, gameId),
    gameEngine = gameEngine,
    // The process lifecycle, unlike the screen's, does not stop across configuration changes.
    appInForeground = ProcessLifecycleOwner.get().lifecycle.currentStateFlow.map {
      it.isAtLeast(Lifecycle.State.STARTED)
    },
  ) {
  @AssistedFactory
  interface Factory {
    fun create(gameId: String): OnlineGameViewModel
  }

  init {
    launchIn(viewModelScope)
  }
}
