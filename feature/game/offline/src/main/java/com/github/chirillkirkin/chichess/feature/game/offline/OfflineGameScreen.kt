package com.github.chirillkirkin.chichess.feature.game.offline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.chirillkirkin.chichess.core.designsystem.theme.ChiChessTheme
import com.github.chirillkirkin.chichess.feature.game.board.BoardMessage
import com.github.chirillkirkin.chichess.feature.game.board.ChessBoard
import com.github.chirillkirkin.chichess.feature.game.domain.DrawReason
import com.github.chirillkirkin.chichess.feature.game.domain.GameStatus
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor

private val GameResultSpace = 64.dp

@Composable
fun OfflineGameRoot(modifier: Modifier = Modifier, viewModel: OfflineGameViewModel = hiltViewModel()) {
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
  val resultText = gameResultText(state.gameStatus)

  BoxWithConstraints(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    val canClaimDraw = resultText == null && state.claimableDrawReason != null
    val resultSpace = GameResultSpace.coerceAtMost(maxHeight)
    val boardSize = minOf(maxWidth, (maxHeight - resultSpace).coerceAtLeast(0.dp))

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      ChessBoard(
        state = state.board,
        onSquareClick = { square ->
          onMessage(OfflineGameMessage.Board(BoardMessage.SquareClick(square)))
        },
        modifier = Modifier.size(boardSize),
        promotionSquare = state.pendingPromotion?.to,
        onPromotionSelect = { piece -> onMessage(OfflineGameMessage.PromotionSelected(piece)) },
        onPromotionDismiss = { onMessage(OfflineGameMessage.PromotionDismissed) },
      )

      Box(
        modifier = Modifier.size(width = boardSize, height = resultSpace),
        contentAlignment = Alignment.TopCenter,
      ) {
        if (resultText != null) {
          Text(
            text = resultText,
            modifier = Modifier.padding(top = ChiChessTheme.spacing.medium),
            style = ChiChessTheme.typography.gameResult,
          )
        } else if (canClaimDraw) {
          Button(
            onClick = { onMessage(OfflineGameMessage.ClaimDraw) },
            modifier = Modifier.padding(top = ChiChessTheme.spacing.medium),
          ) {
            Text(stringResource(R.string.claim_draw))
          }
        }
      }
    }
  }
}

@Composable
@ReadOnlyComposable
private fun gameResultText(status: GameStatus): String? = when (status) {
  GameStatus.Ongoing -> null

  is GameStatus.Checkmate ->
    stringResource(
      when (status.winner) {
        PieceColor.WHITE -> R.string.white_wins_by_checkmate
        PieceColor.BLACK -> R.string.black_wins_by_checkmate
      },
    )

  is GameStatus.Draw ->
    stringResource(
      when (status.reason) {
        DrawReason.STALEMATE -> R.string.draw_by_stalemate
        DrawReason.THREEFOLD_REPETITION -> R.string.draw_by_threefold_repetition
        DrawReason.FIFTY_MOVE_RULE -> R.string.draw_by_fifty_move_rule
        DrawReason.FIVEFOLD_REPETITION -> R.string.draw_by_fivefold_repetition
        DrawReason.SEVENTY_FIVE_MOVE_RULE -> R.string.draw_by_seventy_five_move_rule
        DrawReason.INSUFFICIENT_MATERIAL -> R.string.draw_by_insufficient_material
      },
    )
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
