package com.github.chirillkirkin.chichess.feature.game.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import com.github.chirillkirkin.chichess.feature.game.domain.ChessFile
import com.github.chirillkirkin.chichess.feature.game.domain.ChessRank
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.Square
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BoardOrientationTest {
  @Test
  fun `square at a point follows the white perspective`() {
    assertEquals(Square(ChessFile.A, ChessRank.EIGHT), White.squareAt(Offset(1f, 1f), SquarePx))
    assertEquals(Square(ChessFile.H, ChessRank.ONE), White.squareAt(Offset(79f, 79f), SquarePx))
  }

  @Test
  fun `square at a point follows the black perspective`() {
    assertEquals(Square(ChessFile.H, ChessRank.ONE), Black.squareAt(Offset(1f, 1f), SquarePx))
    assertEquals(Square(ChessFile.A, ChessRank.EIGHT), Black.squareAt(Offset(79f, 79f), SquarePx))
  }

  @Test
  fun `points outside the board have no square`() {
    assertNull(White.squareAt(Offset(-1f, 1f), SquarePx))
    assertNull(White.squareAt(Offset(1f, -1f), SquarePx))
    assertNull(White.squareAt(Offset(80f, 1f), SquarePx))
    assertNull(White.squareAt(Offset(1f, 80f), SquarePx))
  }

  @Test
  fun `square offset is the top left corner of the square`() {
    val square = Square(ChessFile.B, ChessRank.THREE)

    assertEquals(IntOffset(10, 50), White.squareOffset(square, SquarePx))
    assertEquals(IntOffset(60, 20), Black.squareOffset(square, SquarePx))
  }

  @Test
  fun `every square center maps back to its square`() {
    listOf(White, Black).forEach { orientation ->
      ChessRank.entries.forEach { rank ->
        ChessFile.entries.forEach { file ->
          val square = Square(file, rank)

          assertEquals(square, orientation.squareAt(orientation.squareCenter(square, SquarePx), SquarePx))
        }
      }
    }
  }

  private companion object {
    const val SquarePx = 10f
    val White = BoardOrientation(PieceColor.WHITE)
    val Black = BoardOrientation(PieceColor.BLACK)
  }
}
