package com.github.chirillkirkin.chichess.feature.game.clock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.chirillkirkin.chichess.core.designsystem.theme.ChiChessTheme
import com.github.chirillkirkin.chichess.feature.game.domain.TimeControl
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

private val InitialMinutesRange = TimeControl.InitialRange.toIntRange(DurationUnit.MINUTES)

private val IncrementSecondsRange = TimeControl.IncrementRange.toIntRange(DurationUnit.SECONDS)

// Fits the label, a slider long enough to drag precisely, and the number field.
private val SliderMinWidth = 280.dp
private const val SideBySideSliderWeight = 1f

private val TimeControlSaver =
  Saver<TimeControl, List<Long>>(save = { it.toSaved() }, restore = { it.toTimeControl() })

// Saves the state holder itself, so a game without a clock survives as null instead of the initial value.
private val TimeControlStateSaver =
  Saver<MutableState<TimeControl?>, List<Long>>(
    save = { state -> state.value?.toSaved().orEmpty() },
    restore = { saved -> mutableStateOf(saved.takeIf { it.isNotEmpty() }?.toTimeControl()) },
  )

@Composable
fun rememberTimeControlState(initial: TimeControl?): MutableState<TimeControl?> =
  rememberSaveable(saver = TimeControlStateSaver) { mutableStateOf(initial) }

/** A null [timeControl] means a game without a clock; [presets] offer common time controls in one tap. */
@Composable
fun TimeControlPicker(
  timeControl: TimeControl?,
  onTimeControlChange: (TimeControl?) -> Unit,
  modifier: Modifier = Modifier,
  presets: List<TimeControl> = emptyList(),
  enabled: Boolean = true,
) {
  var lastTimeControl by rememberSaveable(stateSaver = TimeControlSaver) {
    mutableStateOf(timeControl ?: TimeControl.Default)
  }
  SideEffect {
    if (timeControl != null) lastTimeControl = timeControl
  }
  // The controls keep showing the last values while they animate out.
  val shownTimeControl = timeControl ?: lastTimeControl

  Column(modifier = modifier) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(vertical = ChiChessTheme.spacing.small),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(text = stringResource(R.string.time_control_with_clock))
      Switch(
        checked = timeControl != null,
        onCheckedChange = { checked -> onTimeControlChange(if (checked) lastTimeControl else null) },
        enabled = enabled,
      )
    }

    AnimatedVisibility(visible = timeControl != null) {
      Column(verticalArrangement = Arrangement.spacedBy(ChiChessTheme.spacing.small)) {
        if (presets.isNotEmpty()) {
          TimeControlPresets(
            presets = presets,
            selected = timeControl,
            onPresetClick = onTimeControlChange,
            enabled = enabled,
          )
        }

        TimeControlSliders(
          timeControl = shownTimeControl,
          onTimeControlChange = onTimeControlChange,
          enabled = enabled,
        )
      }
    }
  }
}

@Composable
private fun TimeControlPresets(
  presets: List<TimeControl>,
  selected: TimeControl?,
  onPresetClick: (TimeControl) -> Unit,
  enabled: Boolean,
) {
  FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(ChiChessTheme.spacing.small, Alignment.CenterHorizontally),
  ) {
    presets.forEach { preset ->
      FilterChip(
        selected = preset == selected,
        onClick = { onPresetClick(preset) },
        label = {
          Text(
            text =
              stringResource(
                R.string.time_control_preset,
                preset.initial.inWholeMinutes,
                preset.increment.inWholeSeconds,
              ),
          )
        },
        enabled = enabled,
      )
    }
  }
}

// Side by side when both sliders fit at a usable width, stacked otherwise.
@Composable
private fun TimeControlSliders(timeControl: TimeControl, onTimeControlChange: (TimeControl) -> Unit, enabled: Boolean) {
  BoxWithConstraints {
    val gap = ChiChessTheme.spacing.medium

    if (maxWidth >= SliderMinWidth * 2 + gap) {
      Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        MinutesSlider(timeControl, onTimeControlChange, enabled, Modifier.weight(SideBySideSliderWeight))
        IncrementSlider(timeControl, onTimeControlChange, enabled, Modifier.weight(SideBySideSliderWeight))
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(ChiChessTheme.spacing.small)) {
        MinutesSlider(timeControl, onTimeControlChange, enabled)
        IncrementSlider(timeControl, onTimeControlChange, enabled)
      }
    }
  }
}

@Composable
private fun MinutesSlider(
  timeControl: TimeControl,
  onTimeControlChange: (TimeControl) -> Unit,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  NumberSlider(
    label = stringResource(R.string.time_control_minutes),
    value = timeControl.initial.inWholeMinutes.toInt(),
    range = InitialMinutesRange,
    onValueChange = { minutes -> onTimeControlChange(timeControl.copy(initial = minutes.minutes)) },
    modifier = modifier,
    enabled = enabled,
  )
}

@Composable
private fun IncrementSlider(
  timeControl: TimeControl,
  onTimeControlChange: (TimeControl) -> Unit,
  enabled: Boolean,
  modifier: Modifier = Modifier,
) {
  NumberSlider(
    label = stringResource(R.string.time_control_increment),
    value = timeControl.increment.inWholeSeconds.toInt(),
    range = IncrementSecondsRange,
    onValueChange = { seconds -> onTimeControlChange(timeControl.copy(increment = seconds.seconds)) },
    modifier = modifier,
    enabled = enabled,
  )
}

@Preview(showBackground = true)
@Composable
private fun TimeControlPickerPreview() {
  ChiChessTheme(dynamicColor = false) {
    TimeControlPicker(
      timeControl = TimeControl(initial = 5.minutes, increment = 3.seconds),
      onTimeControlChange = {},
      presets = listOf(TimeControl(initial = 3.minutes, increment = 2.seconds), TimeControl.Default),
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun TimeControlPickerWithoutClockPreview() {
  ChiChessTheme(dynamicColor = false) {
    TimeControlPicker(timeControl = null, onTimeControlChange = {})
  }
}

private fun TimeControl.toSaved(): List<Long> = listOf(initial.inWholeMilliseconds, increment.inWholeMilliseconds)

private fun List<Long>.toTimeControl(): TimeControl {
  val (initial, increment) = this
  return TimeControl(initial = initial.milliseconds, increment = increment.milliseconds)
}

private fun ClosedRange<Duration>.toIntRange(unit: DurationUnit): IntRange = start.toInt(unit)..endInclusive.toInt(unit)
