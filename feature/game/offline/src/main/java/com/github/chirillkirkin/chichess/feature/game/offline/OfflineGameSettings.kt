package com.github.chirillkirkin.chichess.feature.game.offline

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.github.chirillkirkin.chichess.core.designsystem.theme.ChiChessTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OfflineGameSettingsSheet(
  boardLayout: OfflineBoardLayout,
  onBoardLayoutSelect: (OfflineBoardLayout) -> Unit,
  onNewGameClick: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    modifier = modifier,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = ChiChessTheme.spacing.medium)
          .padding(bottom = ChiChessTheme.spacing.medium),
      verticalArrangement = Arrangement.spacedBy(ChiChessTheme.spacing.small),
    ) {
      Button(
        onClick = onNewGameClick,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(stringResource(R.string.new_game))
      }

      Text(
        text = stringResource(R.string.board_layout_title),
        modifier = Modifier.padding(top = ChiChessTheme.spacing.small),
        style = ChiChessTheme.typography.settingsSectionTitle,
      )

      Column(modifier = Modifier.selectableGroup()) {
        OfflineBoardLayout.entries.forEach { layout ->
          BoardLayoutOption(
            label = layout.label(),
            selected = layout == boardLayout,
            onClick = { onBoardLayoutSelect(layout) },
          )
        }
      }
    }
  }
}

@Composable
private fun BoardLayoutOption(
  @StringRes label: Int,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
        .padding(vertical = ChiChessTheme.spacing.small),
    horizontalArrangement = Arrangement.spacedBy(ChiChessTheme.spacing.small),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(selected = selected, onClick = null)
    Text(stringResource(label))
  }
}

@Composable
internal fun NewGameConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
  AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text(stringResource(R.string.new_game))
      }
    },
    modifier = modifier,
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.cancel))
      }
    },
    title = { Text(stringResource(R.string.new_game_confirmation_title)) },
    text = { Text(stringResource(R.string.new_game_confirmation_text)) },
  )
}

@StringRes
private fun OfflineBoardLayout.label(): Int = when (this) {
  OfflineBoardLayout.STANDARD -> R.string.board_layout_standard
  OfflineBoardLayout.BLACK_UPSIDE_DOWN -> R.string.board_layout_black_upside_down
  OfflineBoardLayout.FLIP_AFTER_MOVE -> R.string.board_layout_flip_after_move
}
