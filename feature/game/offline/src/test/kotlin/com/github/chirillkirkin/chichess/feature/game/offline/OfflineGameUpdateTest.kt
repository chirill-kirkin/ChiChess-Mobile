package com.github.chirillkirkin.chichess.feature.game.offline

import com.github.chirillkirkin.chichess.feature.game.board.BoardMessage
import com.github.chirillkirkin.chichess.feature.game.domain.ChessFile
import com.github.chirillkirkin.chichess.feature.game.domain.ChessRank
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.Square
import com.github.chirillkirkin.chichess.feature.game.engine.ChesslibGameEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OfflineGameUpdateTest {
  private val update = offlineGameUpdate(ChesslibGameEngine())

  @Test
  fun `destination click moves selected piece and clears selection`() {
    val origin = Square(ChessFile.E, ChessRank.TWO)
    val destination = Square(ChessFile.E, ChessRank.FOUR)
    val initialState = OfflineGameState()
    val movingPiece = initialState.board.position[origin]

    val selectedState = initialState.reduceBoardClick(origin)
    val movedState = selectedState.reduceBoardClick(destination)

    assertEquals(origin, selectedState.board.selectedSquare)
    assertNull(movedState.board.position[origin])
    assertEquals(movingPiece, movedState.board.position[destination])
    assertNull(movedState.board.selectedSquare)
    assertEquals(PieceColor.BLACK, movedState.board.position.sideToMove)
  }

  @Test
  fun `clicking another friendly piece changes selection without moving`() {
    val initiallySelectedSquare = Square(ChessFile.E, ChessRank.TWO)
    val newlySelectedSquare = Square(ChessFile.D, ChessRank.TWO)
    val initialState = OfflineGameState()
    val initialPosition = initialState.board.position

    val selectedState = initialState.reduceBoardClick(initiallySelectedSquare)
    val reselectedState = selectedState.reduceBoardClick(newlySelectedSquare)

    assertEquals(newlySelectedSquare, reselectedState.board.selectedSquare)
    assertEquals(initialPosition, reselectedState.board.position)
  }

  @Test
  fun `illegal destination keeps the current selection and position`() {
    val origin = Square(ChessFile.E, ChessRank.TWO)
    val illegalDestination = Square(ChessFile.E, ChessRank.FIVE)
    val initialState = OfflineGameState()
    val selectedState = initialState.reduceBoardClick(origin)

    val rejectedState = selectedState.reduceBoardClick(illegalDestination)

    assertEquals(selectedState, rejectedState)
  }

  private fun OfflineGameState.reduceBoardClick(square: Square): OfflineGameState =
    update(
      OfflineGameMessage.Board(BoardMessage.SquareClick(square)),
      this,
    ).state
}
