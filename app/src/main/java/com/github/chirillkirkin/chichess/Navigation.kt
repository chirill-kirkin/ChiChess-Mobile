package com.github.chirillkirkin.chichess

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.github.chirillkirkin.chichess.core.designsystem.theme.ChiChessTheme
import com.github.chirillkirkin.chichess.feature.game.offline.OfflineGameRoot
import com.github.chirillkirkin.chichess.feature.game.online.presentation.game.OnlineGameRoot
import com.github.chirillkirkin.chichess.feature.game.online.presentation.lobby.OnlineLobbyRoot
import com.github.chirillkirkin.chichess.feature.home.presentation.HomeRoot

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(HomeRoute)
  val appName = stringResource(R.string.app_name)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryDecorators =
      listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
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
          OfflineGameRoot(modifier = Modifier.safeDrawingPadding())
        }
        entry<OnlineLobbyRoute> {
          OnlineLobbyRoot(
            onOpenGame = { gameId -> backStack.add(OnlineGameRoute(gameId)) },
            modifier =
              Modifier
                .safeDrawingPadding()
                .padding(ChiChessTheme.spacing.medium),
          )
        }
        entry<OnlineGameRoute> { key ->
          OnlineGameRoot(gameId = key.gameId, modifier = Modifier.safeDrawingPadding())
        }
      },
  )
}
