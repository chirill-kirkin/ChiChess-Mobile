package com.github.chirillkirkin.chichess.feature.game.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ChessModelsTest {
  @Test
  fun `every square has its expected color`() {
    ChessRank.entries.forEachIndexed { rankIndex, rank ->
      ChessFile.entries.forEachIndexed { fileIndex, file ->
        val square = Square(file, rank)

        assertEquals(
          expectedSquareColors[rankIndex][fileIndex],
          square.color,
          "Unexpected color for $square",
        )
      }
    }
  }

  @Test
  fun `initial position matches the standard setup on every square`() {
    val position = initialChessPosition()

    assertEquals(
      Fen(ExpectedInitialFenValue),
      position.fen,
    )
    assertEquals(PieceColor.WHITE, position.sideToMove)
    allSquares.forEach { square ->
      assertEquals(
        expectedInitialPiece(square),
        position[square],
        "Unexpected initial piece on $square",
      )
    }
  }

  private fun expectedInitialPiece(square: Square): ChessPiece? = when (square.rank) {
    ChessRank.ONE -> ChessPiece(PieceColor.WHITE, backRankPieceTypes[square.file.ordinal])
    ChessRank.TWO -> ChessPiece(PieceColor.WHITE, PieceType.PAWN)
    ChessRank.SEVEN -> ChessPiece(PieceColor.BLACK, PieceType.PAWN)
    ChessRank.EIGHT -> ChessPiece(PieceColor.BLACK, backRankPieceTypes[square.file.ordinal])
    ChessRank.THREE, ChessRank.FOUR, ChessRank.FIVE, ChessRank.SIX -> null
  }

  private companion object {
    const val ExpectedInitialFenValue =
      "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    val darkFirstRankColors: List<SquareColor> =
      listOf(
        SquareColor.DARK,
        SquareColor.LIGHT,
        SquareColor.DARK,
        SquareColor.LIGHT,
        SquareColor.DARK,
        SquareColor.LIGHT,
        SquareColor.DARK,
        SquareColor.LIGHT,
      )

    val lightFirstRankColors: List<SquareColor> =
      listOf(
        SquareColor.LIGHT,
        SquareColor.DARK,
        SquareColor.LIGHT,
        SquareColor.DARK,
        SquareColor.LIGHT,
        SquareColor.DARK,
        SquareColor.LIGHT,
        SquareColor.DARK,
      )

    val expectedSquareColors: List<List<SquareColor>> =
      listOf(
        darkFirstRankColors,
        lightFirstRankColors,
        darkFirstRankColors,
        lightFirstRankColors,
        darkFirstRankColors,
        lightFirstRankColors,
        darkFirstRankColors,
        lightFirstRankColors,
      )

    val allSquares: List<Square> =
      ChessRank.entries.flatMap { rank ->
        ChessFile.entries.map { file -> Square(file, rank) }
      }

    val backRankPieceTypes: List<PieceType> =
      listOf(
        PieceType.ROOK,
        PieceType.KNIGHT,
        PieceType.BISHOP,
        PieceType.QUEEN,
        PieceType.KING,
        PieceType.BISHOP,
        PieceType.KNIGHT,
        PieceType.ROOK,
      )
  }
}
