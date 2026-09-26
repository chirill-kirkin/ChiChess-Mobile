package com.github.chirillkirkin.chichess

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.github.chirillkirkin.chichess.core.designsystem.theme.ChiChessTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // The launcher relaunches a fresh MainActivity on top of the running task instead of resuming
    // it (a long-standing framework bug when the app was first started from the IDE or the icon),
    // which would drop the in-memory back stack. Defer to the existing instance in that case.
    if (!isTaskRoot && Intent.ACTION_MAIN == intent.action && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
      finish()
      return
    }

    enableEdgeToEdge()
    setContent {
      ChiChessTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background,
        ) {
          MainNavigation()
        }
      }
    }
  }
}
