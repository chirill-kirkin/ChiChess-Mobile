package com.github.chirillkirkin.chichess.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

@Immutable
data class ChiChessColors(
  val lightBoardSquare: Color,
  val darkBoardSquare: Color,
  val boardSelectionOverlay: Color,
  val boardLegalTargetOverlay: Color,
  val boardCheckedKingOverlay: Color,
)

internal val DefaultChiChessColors =
  ChiChessColors(
    lightBoardSquare = Color(0xFFF0D9B5),
    darkBoardSquare = Color(0xFFB58863),
    boardSelectionOverlay = Color(0x80F6F669),
    boardLegalTargetOverlay = Color(0x663A7D44),
    boardCheckedKingOverlay = Color(0xB3D32F2F),
  )
