package com.github.chirillkirkin.chichess.feature.game.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimeoutTest {
  @Test
  fun `a lone king cannot checkmate`() {
    assertTrue(position().hasInsufficientMatingMaterial(PieceColor.WHITE))
  }

  @Test
  fun `a single minor piece cannot checkmate`() {
    assertTrue(position(WHITE_KNIGHT at "c3").hasInsufficientMatingMaterial(PieceColor.WHITE))
    assertTrue(position(WHITE_BISHOP at "c1").hasInsufficientMatingMaterial(PieceColor.WHITE))
  }

  @Test
  fun `bishops on squares of one color cannot checkmate`() {
    assertTrue(position(WHITE_BISHOP at "c1", WHITE_BISHOP at "e3").hasInsufficientMatingMaterial(PieceColor.WHITE))
  }

  @Test
  fun `bishops on squares of both colors can checkmate`() {
    assertFalse(position(WHITE_BISHOP at "c1", WHITE_BISHOP at "f1").hasInsufficientMatingMaterial(PieceColor.WHITE))
  }

  @Test
  fun `two minor pieces including a knight can checkmate`() {
    assertFalse(position(WHITE_KNIGHT at "c3", WHITE_BISHOP at "c1").hasInsufficientMatingMaterial(PieceColor.WHITE))
  }

  @Test
  fun `a pawn can checkmate`() {
    assertFalse(position(WHITE_PAWN at "a2").hasInsufficientMatingMaterial(PieceColor.WHITE))
  }

  @Test
  fun `a lone king cannot checkmate even when the opponent has pieces`() {
    assertTrue(position(BLACK_PAWN at "a7").hasInsufficientMatingMaterial(PieceColor.WHITE))
  }

  @Test
  fun `a knight can checkmate a king hemmed in by its own pieces`() {
    assertFalse(position(WHITE_KNIGHT at "c3", BLACK_PAWN at "a7").hasInsufficientMatingMaterial(PieceColor.WHITE))
    assertFalse(position(WHITE_KNIGHT at "c3", BLACK_ROOK at "a8").hasInsufficientMatingMaterial(PieceColor.WHITE))
  }

  @Test
  fun `a knight cannot checkmate a king with only queens`() {
    assertTrue(position(WHITE_KNIGHT at "c3", BLACK_QUEEN at "d8").hasInsufficientMatingMaterial(PieceColor.WHITE))
  }

  @Test
  fun `a bishop can checkmate a king with a knight or a pawn`() {
    assertFalse(position(WHITE_BISHOP at "c1", BLACK_KNIGHT at "b8").hasInsufficientMatingMaterial(PieceColor.WHITE))
    assertFalse(position(WHITE_BISHOP at "c1", BLACK_PAWN at "a7").hasInsufficientMatingMaterial(PieceColor.WHITE))
  }

  @Test
  fun `a bishop can checkmate a king with a bishop on the other color`() {
    assertFalse(position(WHITE_BISHOP at "c1", BLACK_BISHOP at "c8").hasInsufficientMatingMaterial(PieceColor.WHITE))
  }

  @Test
  fun `a bishop cannot checkmate a king with a bishop on the same color or a rook`() {
    assertTrue(position(WHITE_BISHOP at "c1", BLACK_BISHOP at "f8").hasInsufficientMatingMaterial(PieceColor.WHITE))
    assertTrue(position(WHITE_BISHOP at "c1", BLACK_ROOK at "a8").hasInsufficientMatingMaterial(PieceColor.WHITE))
  }

  @Test
  fun `a flag loses when the opponent can checkmate`() {
    assertEquals(
      GameStatus.Timeout(winner = PieceColor.BLACK),
      position(BLACK_PAWN at "a7").timeoutStatus(flagged = PieceColor.WHITE),
    )
  }

  @Test
  fun `a flag draws when the opponent cannot checkmate`() {
    assertEquals(
      GameStatus.Draw(DrawReason.TIMEOUT_VS_INSUFFICIENT_MATERIAL),
      position(WHITE_PAWN at "a2").timeoutStatus(flagged = PieceColor.WHITE),
    )
  }

  @Test
  fun `a flag against a knight loses when the flagged side has a pawn`() {
    assertEquals(
      GameStatus.Timeout(winner = PieceColor.WHITE),
      position(WHITE_KNIGHT at "c3", BLACK_PAWN at "a7").timeoutStatus(flagged = PieceColor.BLACK),
    )
  }

  private fun position(vararg pieces: Pair<Square, ChessPiece>): ChessPosition = ChessPosition.fromSnapshot(
    fen = Fen("irrelevant"),
    pieces = mapOf(square("e1") to WHITE_KING, square("e8") to BLACK_KING) + pieces,
    sideToMove = PieceColor.WHITE,
  )

  private infix fun ChessPiece.at(notation: String): Pair<Square, ChessPiece> = square(notation) to this

  private fun square(notation: String): Square = Square(
    file = ChessFile.entries.single { it.notation == notation[0] },
    rank = ChessRank.entries.single { it.notation == notation[1].digitToInt() },
  )

  private companion object {
    val WHITE_KING = ChessPiece(PieceColor.WHITE, PieceType.KING)
    val WHITE_KNIGHT = ChessPiece(PieceColor.WHITE, PieceType.KNIGHT)
    val WHITE_BISHOP = ChessPiece(PieceColor.WHITE, PieceType.BISHOP)
    val WHITE_PAWN = ChessPiece(PieceColor.WHITE, PieceType.PAWN)
    val BLACK_KING = ChessPiece(PieceColor.BLACK, PieceType.KING)
    val BLACK_PAWN = ChessPiece(PieceColor.BLACK, PieceType.PAWN)
    val BLACK_KNIGHT = ChessPiece(PieceColor.BLACK, PieceType.KNIGHT)
    val BLACK_BISHOP = ChessPiece(PieceColor.BLACK, PieceType.BISHOP)
    val BLACK_ROOK = ChessPiece(PieceColor.BLACK, PieceType.ROOK)
    val BLACK_QUEEN = ChessPiece(PieceColor.BLACK, PieceType.QUEEN)
  }
}
