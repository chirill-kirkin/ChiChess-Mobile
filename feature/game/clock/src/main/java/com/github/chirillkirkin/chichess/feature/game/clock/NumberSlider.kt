package com.github.chirillkirkin.chichess.feature.game.clock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.chirillkirkin.chichess.core.designsystem.theme.ChiChessTheme
import kotlin.math.roundToInt

// Fits three digits within the field's content padding.
private val NumberFieldWidth = 72.dp
private const val SliderWeight = 1f

@Composable
internal fun NumberSlider(
  label: String,
  value: Int,
  range: IntRange,
  onValueChange: (Int) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  Column(modifier = modifier) {
    Text(text = label)

    Row(
      horizontalArrangement = Arrangement.spacedBy(ChiChessTheme.spacing.small),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Slider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt()) },
        modifier = Modifier.weight(SliderWeight),
        enabled = enabled,
        valueRange = range.first.toFloat()..range.last.toFloat(),
      )

      NumberField(
        label = label,
        value = value,
        range = range,
        onValueChange = onValueChange,
        modifier = Modifier.width(NumberFieldWidth),
        enabled = enabled,
      )
    }
  }
}

@Composable
private fun NumberField(
  label: String,
  value: Int,
  range: IntRange,
  onValueChange: (Int) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val focusManager = LocalFocusManager.current
  val maxDigits = range.last.toString().length
  var text by rememberSaveable { mutableStateOf(value.toString()) }
  var isFocused by remember { mutableStateOf(false) }

  // While typing, the text stays as entered; otherwise it follows the value, e.g. when the slider moves.
  SideEffect {
    if (!isFocused) text = value.toString()
  }

  OutlinedTextField(
    value = text,
    onValueChange = { input ->
      // Applied while typing, so a value is not lost when the user confirms without leaving the field.
      val typed = input
        .filter(Char::isDigit)
        .take(maxDigits)
        .toIntOrNull()
        ?.coerceIn(range)
      // The field shows exactly the applied value; only an emptied field waits for input.
      text = typed?.toString().orEmpty()
      if (typed != null && typed != value) onValueChange(typed)
    },
    modifier =
      modifier
        .semantics { contentDescription = label }
        .onFocusChanged { focusState ->
          isFocused = focusState.isFocused
          // Empty or out-of-range input shows the applied value once editing ends.
          if (!focusState.isFocused) text = value.toString()
        },
    enabled = enabled,
    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
  )
}
