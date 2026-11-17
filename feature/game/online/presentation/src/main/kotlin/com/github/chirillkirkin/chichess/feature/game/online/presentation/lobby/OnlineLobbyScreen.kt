package com.github.chirillkirkin.chichess.feature.game.online.presentation.lobby

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.github.chirillkirkin.chichess.feature.game.clock.TimeControlPicker
import com.github.chirillkirkin.chichess.feature.game.domain.TimeControl
import com.github.chirillkirkin.chichess.feature.game.online.presentation.R
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val TimeControlPresets =
  listOf(1 to 0, 2 to 1, 3 to 0, 3 to 2, 5 to 0, 5 to 3, 10 to 0, 10 to 5, 15 to 10, 30 to 0)
    .map { (minutes, seconds) -> TimeControl(initial = minutes.minutes, increment = seconds.seconds) }

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
  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    // Scrolls only when the content does not fit, and is centered while it does.
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .heightIn(min = maxHeight)
          .padding(horizontal = ChiChessTheme.spacing.medium),
      verticalArrangement = Arrangement.spacedBy(ChiChessTheme.spacing.medium, Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      TimeControlPicker(
        timeControl = state.timeControl,
        onTimeControlChange = { onMessage(OnlineLobbyMessage.TimeControlChanged(it)) },
        modifier = Modifier.fillMaxWidth(),
        presets = TimeControlPresets,
        enabled = !state.busy,
      )

      GameActions(state = state, onMessage = onMessage)

      state.error?.let { Text(text = stringResource(lobbyErrorRes(it))) }

      if (state.busy) CircularProgressIndicator()
    }
  }
}

// The actions share one row where it fits, so a wide lobby needs no scrolling; a narrow one wraps them.
@Composable
private fun GameActions(state: OnlineLobbyState, onMessage: (OnlineLobbyMessage) -> Unit) {
  FlowRow(
    horizontalArrangement = Arrangement.spacedBy(ChiChessTheme.spacing.medium, Alignment.CenterHorizontally),
    verticalArrangement = Arrangement.spacedBy(ChiChessTheme.spacing.medium),
    itemVerticalAlignment = Alignment.CenterVertically,
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
  }
}

@StringRes
private fun lobbyErrorRes(error: LobbyError): Int = when (error) {
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
