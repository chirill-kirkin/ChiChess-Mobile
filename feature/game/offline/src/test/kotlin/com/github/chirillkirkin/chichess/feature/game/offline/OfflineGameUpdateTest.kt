package com.github.chirillkirkin.chichess.feature.game.offline

import com.github.chirillkirkin.chichess.feature.game.board.BoardMessage
import com.github.chirillkirkin.chichess.feature.game.board.BoardState
import com.github.chirillkirkin.chichess.feature.game.domain.ChessFile
import com.github.chirillkirkin.chichess.feature.game.domain.ChessGameEngine
import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.ChessPiece
import com.github.chirillkirkin.chichess.feature.game.domain.ChessPosition
import com.github.chirillkirkin.chichess.feature.game.domain.ChessRank
import com.github.chirillkirkin.chichess.feature.game.domain.Fen
import com.github.chirillkirkin.chichess.feature.game.domain.MoveApplicationResult
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.PieceType
import com.github.chirillkirkin.chichess.feature.game.domain.PromotionPiece
import com.github.chirillkirkin.chichess.feature.game.domain.Square
import com.github.chirillkirkin.chichess.feature.game.engine.ChesslibGameEngine
import com.github.chirillkirkin.mvu.Update
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OfflineGameUpdateTest {
  private val update = offlineGameUpdate(ChesslibGameEngine())
  private val promotionFrom = Square(ChessFile.A, ChessRank.SEVEN)
  private val promotionTo = Square(ChessFile.A, ChessRank.EIGHT)

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
    assertEquals(emptySet(), movedState.board.legalTargets)
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
  fun `selection exposes only that piece's targets and clears them on deselection`() {
    val firstPiece = Square(ChessFile.E, ChessRank.TWO)
    val secondPiece = Square(ChessFile.D, ChessRank.TWO)
    val firstTarget = Square(ChessFile.E, ChessRank.THREE)
    val secondTarget = Square(ChessFile.D, ChessRank.THREE)
    val fakeEngine =
      object : ChessGameEngine {
        override fun legalMoves(position: ChessPosition): Set<ChessMove> =
          setOf(
            ChessMove(firstPiece, firstTarget),
            ChessMove(secondPiece, secondTarget),
          )

        override fun applyMove(
          position: ChessPosition,
          move: ChessMove,
        ): MoveApplicationResult = error("This test only selects pieces")
      }
    val reducer = offlineGameUpdate(fakeEngine)

    val firstSelection = OfflineGameState().reduceBoardClick(firstPiece, reducer)
    val secondSelection = firstSelection.reduceBoardClick(secondPiece, reducer)
    val deselected = secondSelection.reduceBoardClick(secondPiece, reducer)

    assertEquals(setOf(firstTarget), firstSelection.board.legalTargets)
    assertEquals(setOf(secondTarget), secondSelection.board.legalTargets)
    assertNull(deselected.board.selectedSquare)
    assertEquals(emptySet(), deselected.board.legalTargets)
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

  @Test
  fun `promotion waits for a choice and dismissal leaves the selected pawn in place`() {
    val selectedState = promotionState().reduceBoardClick(promotionFrom)

    val pendingState = selectedState.reduceBoardClick(promotionTo)
    val ignoredBoardClick = pendingState.reduceBoardClick(Square(ChessFile.H, ChessRank.ONE))
    val dismissedState = pendingState.reduceMessage(OfflineGameMessage.PromotionDismissed)

    assertEquals(ChessMove(promotionFrom, promotionTo), pendingState.pendingPromotion)
    assertEquals(selectedState.board, pendingState.board)
    assertEquals(pendingState, ignoredBoardClick)
    assertNull(dismissedState.pendingPromotion)
    assertEquals(selectedState.board, dismissedState.board)
  }

  @Test
  fun `promotion choice applies the move and clears pending state`() {
    val pendingState =
      promotionState()
        .reduceBoardClick(promotionFrom)
        .reduceBoardClick(promotionTo)

    val promotedState =
      pendingState.reduceMessage(OfflineGameMessage.PromotionSelected(PromotionPiece.KNIGHT))

    assertEquals(
      ChessPiece(PieceColor.WHITE, PieceType.KNIGHT),
      promotedState.board.position[promotionTo],
    )
    assertNull(promotedState.board.position[promotionFrom])
    assertEquals(PieceColor.BLACK, promotedState.board.position.sideToMove)
    assertNull(promotedState.board.selectedSquare)
    assertEquals(emptySet(), promotedState.board.legalTargets)
    assertNull(promotedState.pendingPromotion)
  }

  private fun promotionState(): OfflineGameState =
    OfflineGameState(
      board =
        BoardState(
          position =
            ChessPosition.fromSnapshot(
              fen = Fen(PromotionPositionFenValue),
              pieces =
                mapOf(
                  promotionFrom to ChessPiece(PieceColor.WHITE, PieceType.PAWN),
                  Square(ChessFile.H, ChessRank.EIGHT) to ChessPiece(PieceColor.BLACK, PieceType.KING),
                  Square(ChessFile.H, ChessRank.ONE) to ChessPiece(PieceColor.WHITE, PieceType.KING),
                ),
              sideToMove = PieceColor.WHITE,
            ),
        ),
    )

  private fun OfflineGameState.reduceMessage(message: OfflineGameMessage): OfflineGameState =
    update(message, this).state

  private fun OfflineGameState.reduceBoardClick(
    square: Square,
    reducer: Update<OfflineGameMessage, OfflineGameState, Nothing> = update,
  ): OfflineGameState =
    reducer(
      OfflineGameMessage.Board(BoardMessage.SquareClick(square)),
      this,
    ).state

  private companion object {
    const val PromotionPositionFenValue = "7k/P7/8/8/8/8/8/7K w - - 0 1"
  }
}
