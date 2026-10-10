package com.github.chirillkirkin.chichess.feature.game.board

import com.github.chirillkirkin.chichess.feature.game.domain.ChessFile
import com.github.chirillkirkin.chichess.feature.game.domain.ChessPiece
import com.github.chirillkirkin.chichess.feature.game.domain.ChessPosition
import com.github.chirillkirkin.chichess.feature.game.domain.ChessRank
import com.github.chirillkirkin.chichess.feature.game.domain.Fen
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.PieceType
import com.github.chirillkirkin.chichess.feature.game.domain.Square
import com.github.chirillkirkin.chichess.feature.game.domain.initialChessPosition
import kotlin.test.Test
import kotlin.test.assertEquals

class PieceTransitionTest {
  @Test
  fun `identical positions have no transition`() {
    val position = initialChessPosition()

    assertEquals(PieceTransition(emptyMap(), emptyMap()), pieceTransition(position, position))
  }

  @Test
  fun `moved piece is matched to its origin`() {
    val previous = position(E2 to WhitePawn, E1 to WhiteKing)
    val current = position(E4 to WhitePawn, E1 to WhiteKing)

    assertEquals(PieceTransition(mapOf(E4 to E2), emptyMap()), pieceTransition(previous, current))
  }

  @Test
  fun `captured piece vanishes`() {
    val previous = position(E4 to WhitePawn, D5 to BlackPawn)
    val current = position(D5 to WhitePawn)

    assertEquals(PieceTransition(mapOf(D5 to E4), mapOf(D5 to BlackPawn)), pieceTransition(previous, current))
  }

  @Test
  fun `castling moves king and rook`() {
    val previous = position(E1 to WhiteKing, H1 to WhiteRook)
    val current = position(G1 to WhiteKing, F1 to WhiteRook)

    assertEquals(PieceTransition(mapOf(G1 to E1, F1 to H1), emptyMap()), pieceTransition(previous, current))
  }

  @Test
  fun `en passant moves the capturing pawn and the captured pawn vanishes`() {
    val previous = position(E5 to WhitePawn, D5 to BlackPawn)
    val current = position(D6 to WhitePawn)

    assertEquals(PieceTransition(mapOf(D6 to E5), mapOf(D5 to BlackPawn)), pieceTransition(previous, current))
  }

  @Test
  fun `promoted piece is matched to the promoting pawn`() {
    val previous = position(E7 to WhitePawn)
    val current = position(E8 to WhiteQueen)

    assertEquals(PieceTransition(mapOf(E8 to E7), emptyMap()), pieceTransition(previous, current))
  }

  @Test
  fun `promotion with capture is matched to the promoting pawn`() {
    val previous = position(E7 to WhitePawn, D8 to BlackRook)
    val current = position(D8 to WhiteQueen)

    assertEquals(PieceTransition(mapOf(D8 to E7), mapOf(D8 to BlackRook)), pieceTransition(previous, current))
  }

  @Test
  fun `new game returns moved pieces to their initial squares`() {
    val initial = initialChessPosition()
    val previous =
      position(
        *boardSquares()
          .mapNotNull { square -> initial[square]?.let { square to it } }
          .filterNot { (square, _) -> square == E2 || square == G1 }
          .plus(listOf(E4 to WhitePawn, F3 to ChessPiece(PieceColor.WHITE, PieceType.KNIGHT)))
          .toTypedArray(),
      )

    assertEquals(PieceTransition(mapOf(E2 to E4, G1 to F3), emptyMap()), pieceTransition(previous, initial))
  }

  private fun position(vararg pieces: Pair<Square, ChessPiece>): ChessPosition =
    ChessPosition.fromSnapshot(fen = Fen(""), pieces = pieces.toMap(), sideToMove = PieceColor.WHITE)

  private fun boardSquares(): List<Square> =
    ChessRank.entries.flatMap { rank -> ChessFile.entries.map { file -> Square(file, rank) } }

  private companion object {
    val D5 = Square(ChessFile.D, ChessRank.FIVE)
    val D6 = Square(ChessFile.D, ChessRank.SIX)
    val D8 = Square(ChessFile.D, ChessRank.EIGHT)
    val E1 = Square(ChessFile.E, ChessRank.ONE)
    val E2 = Square(ChessFile.E, ChessRank.TWO)
    val E4 = Square(ChessFile.E, ChessRank.FOUR)
    val E5 = Square(ChessFile.E, ChessRank.FIVE)
    val E7 = Square(ChessFile.E, ChessRank.SEVEN)
    val E8 = Square(ChessFile.E, ChessRank.EIGHT)
    val F1 = Square(ChessFile.F, ChessRank.ONE)
    val F3 = Square(ChessFile.F, ChessRank.THREE)
    val G1 = Square(ChessFile.G, ChessRank.ONE)
    val H1 = Square(ChessFile.H, ChessRank.ONE)

    val WhitePawn = ChessPiece(PieceColor.WHITE, PieceType.PAWN)
    val WhiteKing = ChessPiece(PieceColor.WHITE, PieceType.KING)
    val WhiteRook = ChessPiece(PieceColor.WHITE, PieceType.ROOK)
    val WhiteQueen = ChessPiece(PieceColor.WHITE, PieceType.QUEEN)
    val BlackPawn = ChessPiece(PieceColor.BLACK, PieceType.PAWN)
    val BlackRook = ChessPiece(PieceColor.BLACK, PieceType.ROOK)
  }
}
