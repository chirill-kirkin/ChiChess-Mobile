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

  @Test
  fun `drag start selects only active color pieces and keeps the selected square`() {
    val occupiedSquare = Square(ChessFile.E, ChessRank.TWO)
    val inactivePieceSquare = Square(ChessFile.E, ChessRank.SEVEN)
    val emptySquare = Square(ChessFile.E, ChessRank.FOUR)
    val initialState = BoardState(position = initialChessPosition())

    val afterEmptySquareDrag =
      boardUpdate(BoardMessage.PieceDragStart(emptySquare), initialState)
    val afterInactivePieceDrag =
      boardUpdate(BoardMessage.PieceDragStart(inactivePieceSquare), afterEmptySquareDrag)
    val afterPieceDrag =
      boardUpdate(BoardMessage.PieceDragStart(occupiedSquare), afterInactivePieceDrag)
    val afterSelectedPieceDrag =
      boardUpdate(BoardMessage.PieceDragStart(occupiedSquare), afterPieceDrag)

    assertEquals(initialState, afterEmptySquareDrag)
    assertEquals(initialState, afterInactivePieceDrag)
    assertEquals(initialState.copy(selectedSquare = occupiedSquare), afterPieceDrag)
    assertEquals(afterPieceDrag, afterSelectedPieceDrag)
  }

  @Test
  fun `drop clears the selection`() {
    val origin = Square(ChessFile.E, ChessRank.TWO)
    val initialState = BoardState(position = initialChessPosition())
    val selectedState = initialState.copy(selectedSquare = origin)

    val droppedState = boardUpdate(BoardMessage.PieceDrop(origin, Square(ChessFile.E, ChessRank.FIVE)), selectedState)

    assertEquals(initialState, droppedState)
  }
}
