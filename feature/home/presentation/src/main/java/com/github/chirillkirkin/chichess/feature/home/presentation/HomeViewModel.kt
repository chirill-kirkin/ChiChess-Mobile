package com.github.chirillkirkin.chichess.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.chirillkirkin.mvu.MVU
import com.github.chirillkirkin.mvu.MVUStore
import com.github.chirillkirkin.mvu.Update
import com.github.chirillkirkin.mvu.update
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.emptyFlow

data object HomeState

sealed interface HomeMessage {
  data object OfflineGameClick : HomeMessage

  data object OnlineGameClick : HomeMessage
}

sealed interface HomeCommand {
  data object OpenOfflineGame : HomeCommand

  data object OpenOnlineLobby : HomeCommand
}

private val homeUpdate: Update<HomeMessage, HomeState, HomeCommand> =
  update { message, _ ->
    when (message) {
      HomeMessage.OfflineGameClick -> command(HomeCommand.OpenOfflineGame)
      HomeMessage.OnlineGameClick -> command(HomeCommand.OpenOnlineLobby)
    }
  }

@ViewModelScoped
class HomeStore @Inject constructor() :
  MVUStore<HomeMessage, HomeState, HomeCommand>(
    initialState = HomeState,
    update = homeUpdate,
    commandExecutor = { emptyFlow() },
  )

@HiltViewModel
class HomeViewModel @Inject constructor(
  store: HomeStore,
) : ViewModel(), MVU<HomeMessage, HomeState, HomeCommand> by store {
  init {
    launchIn(viewModelScope)
  }
}
