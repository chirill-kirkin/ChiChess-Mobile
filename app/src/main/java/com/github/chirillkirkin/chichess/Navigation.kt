package com.github.chirillkirkin.chichess

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.github.chirillkirkin.chichess.ui.main.MainScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(HomeRoute)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<HomeRoute> {
          MainScreen(
            modifier = Modifier.safeDrawingPadding().padding(16.dp),
          )
        }
      },
  )
}
