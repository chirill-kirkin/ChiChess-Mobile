package com.github.chirillkirkin.chichess.feature.game.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UciTest {
  @Test
  fun `toUci formats a simple move`() {
    val move = ChessMove(Square(ChessFile.E, ChessRank.TWO), Square(ChessFile.E, ChessRank.FOUR))
    assertEquals("e2e4", move.toUci())
  }

  @Test
  fun `toUci appends the promotion symbol`() {
    val move = ChessMove(Square(ChessFile.E, ChessRank.SEVEN), Square(ChessFile.E, ChessRank.EIGHT), PromotionPiece.QUEEN)
    assertEquals("e7e8q", move.toUci())
  }

  @Test
  fun `parseUci reads a simple move`() {
    val expected = ChessMove(Square(ChessFile.E, ChessRank.TWO), Square(ChessFile.E, ChessRank.FOUR))
    assertEquals(expected, parseUci("e2e4"))
  }

  @Test
  fun `parseUci reads a promotion`() {
    val expected = ChessMove(Square(ChessFile.A, ChessRank.SEVEN), Square(ChessFile.A, ChessRank.EIGHT), PromotionPiece.KNIGHT)
    assertEquals(expected, parseUci("a7a8n"))
  }

  @Test
  fun `parseUci round-trips toUci`() {
    val move = ChessMove(Square(ChessFile.G, ChessRank.ONE), Square(ChessFile.F, ChessRank.THREE))
    assertEquals(move, parseUci(move.toUci()))
  }

  @Test
  fun `parseUci rejects malformed input`() {
    assertNull(parseUci(EMPTY))
    assertNull(parseUci(TOO_SHORT))
    assertNull(parseUci(TOO_LONG))
    assertNull(parseUci(INVALID_FILE))
    assertNull(parseUci(INVALID_RANK))
    assertNull(parseUci(INVALID_PROMOTION))
  }

  private companion object {
    const val EMPTY = ""
    const val TOO_SHORT = "e2e"
    const val TOO_LONG = "e2e4qq"
    const val INVALID_FILE = "z2e4"
    const val INVALID_RANK = "e9e4"
    const val INVALID_PROMOTION = "e2e4x"
  }
}
