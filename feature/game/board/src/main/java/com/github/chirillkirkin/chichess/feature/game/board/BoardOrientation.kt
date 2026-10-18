package com.github.chirillkirkin.chichess.feature.game.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import com.github.chirillkirkin.chichess.feature.game.domain.ChessFile
import com.github.chirillkirkin.chichess.feature.game.domain.ChessRank
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.Square
import kotlin.math.floor
import kotlin.math.roundToInt

internal const val SquareCenterFraction = 0.5f

internal data class BoardOrientation(val perspective: PieceColor) {
  val ranks: List<ChessRank> =
    when (perspective) {
      PieceColor.WHITE -> ChessRank.entries.asReversed()
      PieceColor.BLACK -> ChessRank.entries
    }

  val files: List<ChessFile> =
    when (perspective) {
      PieceColor.WHITE -> ChessFile.entries
      PieceColor.BLACK -> ChessFile.entries.asReversed()
    }

  fun squareOffset(square: Square, squarePx: Float): IntOffset = IntOffset(
    x = (files.indexOf(square.file) * squarePx).roundToInt(),
    y = (ranks.indexOf(square.rank) * squarePx).roundToInt(),
  )

  fun squareCenter(square: Square, squarePx: Float): Offset = Offset(
    x = (files.indexOf(square.file) + SquareCenterFraction) * squarePx,
    y = (ranks.indexOf(square.rank) + SquareCenterFraction) * squarePx,
  )

  fun squareAt(offset: Offset, squarePx: Float): Square? {
    val file = files.getOrNull(floor(offset.x / squarePx).toInt()) ?: return null
    val rank = ranks.getOrNull(floor(offset.y / squarePx).toInt()) ?: return null
    return Square(file, rank)
  }
}
