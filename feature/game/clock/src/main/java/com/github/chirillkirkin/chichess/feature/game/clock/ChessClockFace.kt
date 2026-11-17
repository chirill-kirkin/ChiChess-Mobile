package com.github.chirillkirkin.chichess.feature.game.clock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.chirillkirkin.chichess.core.designsystem.theme.ChiChessTheme
import com.github.chirillkirkin.chichess.feature.game.domain.ChessClock
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val ClockTickInterval = 100.milliseconds
private val LowTimeThreshold = 10.seconds
private val ClockFaceShape = RoundedCornerShape(8.dp)
private const val NanosecondsPerTenth = 100_000_000

@Composable
fun ChessClockFace(remaining: Duration, isRunning: Boolean, modifier: Modifier = Modifier) {
  val colors = ChiChessTheme.colors
  val (background, content) =
    when {
      remaining < LowTimeThreshold -> colors.clockLowTimeBackground to colors.clockLowTimeContent
      isRunning -> colors.clockRunningBackground to colors.clockRunningContent
      else -> colors.clockIdleBackground to colors.clockIdleContent
    }

  Surface(
    modifier = modifier,
    shape = ClockFaceShape,
    color = background,
    contentColor = content,
  ) {
    Text(
      text = formatClockTime(remaining),
      modifier =
        Modifier.padding(
          horizontal = ChiChessTheme.spacing.medium,
          vertical = ChiChessTheme.spacing.small,
        ),
      style = ChiChessTheme.typography.clock,
    )
  }
}

@Composable
fun rememberClockRemaining(clock: ChessClock, color: PieceColor): State<Duration> =
  produceState(initialValue = clock.remainingNow(color), clock, color) {
    value = clock.remainingNow(color)
    while (clock.isRunning && clock.activeSide == color && value > Duration.ZERO) {
      delay(ClockTickInterval)
      value = clock.remainingNow(color)
    }
  }

internal fun formatClockTime(remaining: Duration): String =
  remaining.toComponents { hours, minutes, seconds, nanoseconds ->
    when {
      hours > 0 -> String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
      remaining < LowTimeThreshold -> String.format(Locale.ROOT, "%d.%d", seconds, nanoseconds / NanosecondsPerTenth)
      else -> String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
  }

@Preview(showBackground = true)
@Composable
private fun ChessClockFacePreview() {
  ChiChessTheme(dynamicColor = false) {
    Column {
      ChessClockFace(remaining = 5.minutes, isRunning = false)
      ChessClockFace(remaining = 3.minutes + 7.seconds, isRunning = true)
      ChessClockFace(remaining = 7.seconds + 400.milliseconds, isRunning = true)
      ChessClockFace(remaining = 90.minutes, isRunning = false)
    }
  }
}
