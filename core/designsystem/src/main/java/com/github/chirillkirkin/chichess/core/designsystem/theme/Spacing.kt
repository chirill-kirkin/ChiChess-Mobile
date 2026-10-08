package com.github.chirillkirkin.chichess.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ChiChessSpacing(val boardCoordinateInset: Dp, val small: Dp, val medium: Dp)

internal val DefaultChiChessSpacing =
  ChiChessSpacing(
    boardCoordinateInset = 2.dp,
    small = 8.dp,
    medium = 16.dp,
  )
