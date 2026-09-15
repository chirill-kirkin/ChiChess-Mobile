package com.github.chirillkirkin.chichess.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography()

@Immutable
data class ChiChessTypography(
  val boardCoordinate: TextStyle,
  val screenTitle: TextStyle,
)

internal val DefaultChiChessTypography =
  ChiChessTypography(
    boardCoordinate = Typography.labelSmall,
    screenTitle = Typography.bodyLarge,
  )
