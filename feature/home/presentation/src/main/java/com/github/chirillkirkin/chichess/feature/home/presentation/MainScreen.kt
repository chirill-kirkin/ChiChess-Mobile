package com.github.chirillkirkin.chichess.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
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

private const val PreviewAppName = "Chess application"

@Composable
fun HomeRoot(
  appName: String,
  onOpenOfflineGame: () -> Unit,
  onOpenOnlineLobby: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: HomeViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val lifecycleOwner = LocalLifecycleOwner.current
  val currentOpenOfflineGame by rememberUpdatedState(onOpenOfflineGame)
  val currentOpenOnlineLobby by rememberUpdatedState(onOpenOnlineLobby)

  LaunchedEffect(viewModel, lifecycleOwner) {
    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      viewModel.commands.collect { command ->
        when (command) {
          HomeCommand.OpenOfflineGame -> currentOpenOfflineGame()
          HomeCommand.OpenOnlineLobby -> currentOpenOnlineLobby()
        }
      }
    }
  }

  MainScreen(
    appName = appName,
    state = state,
    onMessage = viewModel::send,
    modifier = modifier,
  )
}

@Composable
fun MainScreen(
  appName: String,
  state: HomeState,
  onMessage: (HomeMessage) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement =
      Arrangement.spacedBy(
        space = ChiChessTheme.spacing.medium,
        alignment = Alignment.CenterVertically,
      ),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = appName,
      style = ChiChessTheme.typography.screenTitle,
    )
    Button(onClick = { onMessage(HomeMessage.OfflineGameClick) }) {
      Text(text = stringResource(R.string.play_offline))
    }
    Button(onClick = { onMessage(HomeMessage.OnlineGameClick) }) {
      Text(text = stringResource(R.string.play_online))
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
  ChiChessTheme {
    MainScreen(
      appName = PreviewAppName,
      state = HomeState,
      onMessage = {},
    )
  }
}
