package com.github.chirillkirkin.chichess.feature.game.board

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.chirillkirkin.chichess.core.designsystem.theme.ChiChessTheme

private val SidePanelMinWidth = 240.dp
private val SidePanelMaxWidth = 320.dp

/**
 * A landscape arrangement: the board takes the full height, and [panel] stands to its right as tall as the board.
 * The board stays centered while the panel fits beside it; otherwise both are centered together.
 */
@Composable
fun BoardWithSidePanel(
  board: @Composable (Modifier) -> Unit,
  panel: @Composable (Modifier) -> Unit,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val gap = ChiChessTheme.spacing.medium
    val boardSize = minOf(maxHeight, (maxWidth - SidePanelMinWidth - gap).coerceAtLeast(0.dp))
    val sideSpace = (maxWidth - boardSize) / 2
    val isBoardCentered = sideSpace >= SidePanelMinWidth + gap
    val panelWidth =
      (if (isBoardCentered) sideSpace - gap else maxWidth - boardSize - gap).coerceAtMost(SidePanelMaxWidth)
    val leadingSpace = if (isBoardCentered) sideSpace else (maxWidth - boardSize - gap - panelWidth) / 2

    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
      Spacer(modifier = Modifier.width(leadingSpace))
      board(Modifier.size(boardSize))
      Spacer(modifier = Modifier.width(gap))
      panel(Modifier.size(width = panelWidth, height = boardSize))
    }
  }
}
