package com.github.chirillkirkin.chichess.feature.game.board

import com.github.chirillkirkin.chichess.feature.game.domain.ChessFile
import com.github.chirillkirkin.chichess.feature.game.domain.ChessRank
import com.github.chirillkirkin.chichess.feature.game.domain.Square
import com.github.chirillkirkin.chichess.feature.game.domain.initialChessPosition
import kotlin.test.Test
import kotlin.test.assertEquals

class BoardStateTest {
  @Test
  fun `board selects only active color pieces and toggles selected square`() {
    val occupiedSquare = Square(ChessFile.E, ChessRank.TWO)
    val inactivePieceSquare = Square(ChessFile.E, ChessRank.SEVEN)
    val emptySquare = Square(ChessFile.E, ChessRank.FOUR)
    val initialState = BoardState(position = initialChessPosition())

    val afterEmptySquareClick =
      boardUpdate(BoardMessage.SquareClick(emptySquare), initialState)
    val afterInactivePieceClick =
      boardUpdate(BoardMessage.SquareClick(inactivePieceSquare), afterEmptySquareClick)
    val afterPieceClick =
      boardUpdate(BoardMessage.SquareClick(occupiedSquare), afterInactivePieceClick)
    val afterSelectedPieceClick =
      boardUpdate(BoardMessage.SquareClick(occupiedSquare), afterPieceClick)

    assertEquals(initialState, afterEmptySquareClick)
    assertEquals(initialState, afterInactivePieceClick)
    assertEquals(initialState.copy(selectedSquare = occupiedSquare), afterPieceClick)
    assertEquals(initialState, afterSelectedPieceClick)
  }
}
