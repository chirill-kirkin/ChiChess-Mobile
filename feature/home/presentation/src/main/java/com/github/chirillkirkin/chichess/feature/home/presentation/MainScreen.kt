package com.github.chirillkirkin.chichess.feature.home.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.github.chirillkirkin.chichess.core.designsystem.theme.ChiChessTheme

@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Text(text = "ChiChess")
  }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
  ChiChessTheme {
    MainScreen()
  }
}
