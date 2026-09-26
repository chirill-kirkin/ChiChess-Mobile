package com.github.chirillkirkin.chichess.feature.game.online.presentation.lobby

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.github.chirillkirkin.chichess.core.designsystem.theme.ChiChessTheme
import com.github.chirillkirkin.chichess.feature.game.online.presentation.R

@Composable
fun OnlineLobbyRoot(
  onOpenGame: (String) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: OnlineLobbyViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val lifecycleOwner = LocalLifecycleOwner.current
  val currentOnOpenGame by rememberUpdatedState(onOpenGame)

  LaunchedEffect(viewModel, lifecycleOwner) {
    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      viewModel.commands.collect { command ->
        if (command is OnlineLobbyCommand.OpenGame) currentOnOpenGame(command.gameId)
      }
    }
  }

  OnlineLobbyScreen(state = state, onMessage = viewModel::send, modifier = modifier)
}

@Composable
fun OnlineLobbyScreen(
  state: OnlineLobbyState,
  onMessage: (OnlineLobbyMessage) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(
      space = ChiChessTheme.spacing.medium,
      alignment = Alignment.CenterVertically,
    ),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Button(onClick = { onMessage(OnlineLobbyMessage.CreateGame) }, enabled = !state.busy) {
      Text(text = stringResource(R.string.online_create_game))
    }

    OutlinedTextField(
      value = state.inviteCode,
      onValueChange = { onMessage(OnlineLobbyMessage.InviteCodeChanged(it)) },
      label = { Text(text = stringResource(R.string.online_invite_code_hint)) },
      singleLine = true,
      enabled = !state.busy,
    )

    Button(
      onClick = { onMessage(OnlineLobbyMessage.JoinGame) },
      enabled = !state.busy && state.inviteCode.isNotBlank(),
    ) {
      Text(text = stringResource(R.string.online_join_game))
    }

    state.error?.let { Text(text = stringResource(lobbyErrorRes(it))) }

    if (state.busy) CircularProgressIndicator()
  }
}

@StringRes
private fun lobbyErrorRes(error: LobbyError): Int =
  when (error) {
    LobbyError.NOT_FOUND -> R.string.online_join_error_not_found
    LobbyError.OWN_GAME -> R.string.online_join_error_own_game
    LobbyError.ALREADY_JOINED -> R.string.online_join_error_already_joined
    LobbyError.GENERIC -> R.string.online_join_error_generic
  }

@Preview(showBackground = true)
@Composable
private fun OnlineLobbyScreenPreview() {
  ChiChessTheme(dynamicColor = false) {
    OnlineLobbyScreen(state = OnlineLobbyState(), onMessage = {})
  }
}
