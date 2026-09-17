package com.github.chirillkirkin.chichess.feature.game.offline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.chirillkirkin.chichess.core.designsystem.theme.ChiChessTheme
import com.github.chirillkirkin.chichess.feature.game.board.BoardMessage
import com.github.chirillkirkin.chichess.feature.game.board.ChessBoard

@Composable
fun OfflineGameRoot(
  modifier: Modifier = Modifier,
  viewModel: OfflineGameViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  OfflineGameScreen(
    state = state,
    onMessage = viewModel::send,
    modifier = modifier,
  )
}

@Composable
fun OfflineGameScreen(
  state: OfflineGameState,
  onMessage: (OfflineGameMessage) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    ChessBoard(
      state = state.board,
      onSquareClick = { square ->
        onMessage(OfflineGameMessage.Board(BoardMessage.SquareClick(square)))
      },
      modifier = Modifier.fillMaxSize(),
      promotionSquare = state.pendingPromotion?.to,
      onPromotionSelected = { piece -> onMessage(OfflineGameMessage.PromotionSelected(piece)) },
      onPromotionDismissed = { onMessage(OfflineGameMessage.PromotionDismissed) },
    )
  }
}

@Preview(name = "Phone", widthDp = 360, heightDp = 640, showBackground = true)
@Preview(name = "Tablet landscape", widthDp = 840, heightDp = 600, showBackground = true)
@Composable
private fun OfflineGameScreenPreview() {
  ChiChessTheme(dynamicColor = false) {
    OfflineGameScreen(
      state = OfflineGameState(),
      onMessage = {},
    )
  }
}
