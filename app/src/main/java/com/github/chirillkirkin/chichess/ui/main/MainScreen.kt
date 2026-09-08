package com.github.chirillkirkin.chichess.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.chirillkirkin.chichess.R
import com.github.chirillkirkin.chichess.theme.ChiChessTheme

@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Text(text = stringResource(R.string.app_name))
  }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
  ChiChessTheme {
    MainScreen()
  }
}
