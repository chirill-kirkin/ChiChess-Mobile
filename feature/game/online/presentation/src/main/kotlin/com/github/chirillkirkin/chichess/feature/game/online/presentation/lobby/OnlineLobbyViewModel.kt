package com.github.chirillkirkin.chichess.feature.game.online.presentation.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.chirillkirkin.chelm.MVU
import javax.inject.Inject

@HiltViewModel
class OnlineLobbyViewModel @Inject constructor(store: OnlineLobbyStore) :
  ViewModel(),
  MVU<OnlineLobbyMessage, OnlineLobbyState, OnlineLobbyCommand> by store {
    init {
      launchIn(viewModelScope)
    }
  }
