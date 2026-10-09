package com.github.chirillkirkin.chichess

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.github.chirillkirkin.chichess.core.designsystem.theme.ChiChessTheme
import com.github.chirillkirkin.chichess.feature.game.offline.OfflineGameRoot
import com.github.chirillkirkin.chichess.feature.game.online.presentation.game.OnlineGameRoot
import com.github.chirillkirkin.chichess.feature.game.online.presentation.lobby.LobbyError
import com.github.chirillkirkin.chichess.feature.game.online.presentation.lobby.OnlineLobbyMessage
import com.github.chirillkirkin.chichess.feature.game.online.presentation.lobby.OnlineLobbyRoot
import com.github.chirillkirkin.chichess.feature.game.online.presentation.lobby.OnlineLobbyViewModel
import com.github.chirillkirkin.chichess.feature.home.presentation.HomeRoot

private data object OnlineGameFailed

@Composable
fun MainNavigation(modifier: Modifier = Modifier) {
  val backStack = rememberNavBackStack(HomeRoute)
  val appName = stringResource(R.string.app_name)

  NavDisplay(
    backStack = backStack,
    modifier = modifier,
    onBack = { backStack.removeLastOrNull() },
    entryDecorators =
      listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
        rememberResultEventBusNavEntryDecorator(),
      ),
    entryProvider =
      entryProvider {
        entry<HomeRoute> {
          HomeRoot(
            appName = appName,
            modifier =
              Modifier
                .safeDrawingPadding()
                .padding(ChiChessTheme.spacing.medium),
            onOpenOfflineGame = { backStack.add(OfflineGameRoute) },
            onOpenOnlineLobby = { backStack.add(OnlineLobbyRoute) },
          )
        }
        entry<OfflineGameRoute> {
          OfflineGameRoot(onBack = { backStack.removeLastOrNull() })
        }
        entry<OnlineLobbyRoute> {
          // The entry owns the lobby ViewModel so the game result can reach it through ResultEffect.
          @Suppress("ViewModelInjection")
          val viewModel = hiltViewModel<OnlineLobbyViewModel>()
          ResultEffect<OnlineGameFailed> { viewModel.send(OnlineLobbyMessage.Failed(LobbyError.GENERIC)) }

          OnlineLobbyRoot(
            onOpenGame = { gameId -> backStack.add(OnlineGameRoute(gameId)) },
            modifier =
              Modifier
                .safeDrawingPadding()
                .padding(ChiChessTheme.spacing.medium),
            viewModel = viewModel,
          )
        }
        entry<OnlineGameRoute> { key ->
          val resultBus = LocalResultEventBus.current

          OnlineGameRoot(
            gameId = key.gameId,
            onGameFail = {
              resultBus.sendResult(OnlineGameFailed)
              backStack.removeLastOrNull()
            },
            modifier = Modifier.safeDrawingPadding(),
          )
        }
      },
  )
}
