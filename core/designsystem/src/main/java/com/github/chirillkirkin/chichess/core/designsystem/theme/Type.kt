package com.github.chirillkirkin.chichess.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle

val Typography = Typography()

@Immutable
data class ChiChessTypography(val boardCoordinate: TextStyle, val screenTitle: TextStyle, val gameResult: TextStyle)

internal val DefaultChiChessTypography =
  ChiChessTypography(
    boardCoordinate = Typography.labelSmall,
    screenTitle = Typography.bodyLarge,
    gameResult = Typography.titleMedium,
  )
