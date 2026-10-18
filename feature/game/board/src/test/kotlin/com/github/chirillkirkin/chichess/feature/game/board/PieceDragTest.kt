package com.github.chirillkirkin.chichess.feature.game.board

import androidx.compose.ui.geometry.Offset
import com.github.chirillkirkin.chichess.feature.game.domain.ChessFile
import com.github.chirillkirkin.chichess.feature.game.domain.ChessPosition
import com.github.chirillkirkin.chichess.feature.game.domain.ChessRank
import com.github.chirillkirkin.chichess.feature.game.domain.Fen
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.Square
import com.github.chirillkirkin.chichess.feature.game.domain.initialChessPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PieceDragTest {
  private val drops = mutableListOf<Pair<Square, Square>>()

  @Test
  fun `drag that the selection did not accept is hidden and discarded on drop`() {
    val dragState = dragFromOrigin()

    assertNull(dragState.shownDrag(UnselectedState))

    dragState.end(LegalTarget, UnselectedState, ::recordDrop)

    assertNull(dragState.drag)
    assertEquals(emptyList(), drops)
  }

  @Test
  fun `drop on a legal target holds the piece there and reports the move`() {
    val dragState = dragFromOrigin()

    dragState.end(LegalTarget, SelectedState, ::recordDrop)

    val drag = assertNotNull(dragState.shownDrag(SelectedState))
    assertEquals(PieceDragPhase.Dropped(LegalTarget, SelectedState.position), drag.phase)
    assertEquals(setOf(Origin, LegalTarget), drag.hiddenSquares)
    assertEquals(SquareSizeLift, drag.lift)
    assertEquals(listOf(Origin to LegalTarget), drops)
  }

  @Test
  fun `drop on an illegal square returns the piece and reports the drop`() {
    val dragState = dragFromOrigin()

    dragState.end(IllegalTarget, SelectedState, ::recordDrop)

    assertEquals(PieceDragPhase.Returning, dragState.drag?.phase)
    assertEquals(listOf(Origin to IllegalTarget), drops)
  }

  @Test
  fun `drop on the origin or outside the board returns the piece without a report`() {
    val originDrop = dragFromOrigin()
    val outsideDrop = dragFromOrigin()

    originDrop.end(Origin, SelectedState, ::recordDrop)
    outsideDrop.end(null, SelectedState, ::recordDrop)

    assertEquals(PieceDragPhase.Returning, originDrop.drag?.phase)
    assertEquals(PieceDragPhase.Returning, outsideDrop.drag?.phase)
    assertEquals(emptyList(), drops)
  }

  @Test
  fun `cancel returns an accepted drag and discards an unaccepted one`() {
    val acceptedDrag = dragFromOrigin()
    val unacceptedDrag = dragFromOrigin()

    acceptedDrag.cancel(SelectedState)
    unacceptedDrag.cancel(UnselectedState)

    assertEquals(PieceDragPhase.Returning, acceptedDrag.drag?.phase)
    assertNull(unacceptedDrag.drag)
  }

  @Test
  fun `applied drop finishes the drag`() {
    val dragState = droppedOnLegalTarget()

    dragState.settleDrop(MovedPosition, promotionSquare = null, White, SquarePx)

    assertNull(dragState.drag)
  }

  @Test
  fun `promotion request keeps the dropped piece until it is dismissed`() {
    val dragState = droppedOnLegalTarget()

    dragState.settleDrop(SelectedState.position, promotionSquare = LegalTarget, White, SquarePx)
    val awaitingPhase = dragState.drag?.phase
    val unansweredDrop = dragState.unansweredDrop
    dragState.settleDrop(SelectedState.position, promotionSquare = null, White, SquarePx)

    assertEquals(PieceDragPhase.Dropped(LegalTarget, SelectedState.position, isAwaitingPromotion = true), awaitingPhase)
    assertNull(unansweredDrop)
    assertEquals(PieceDragPhase.Returning, dragState.drag?.phase)
    assertEquals(LegalTargetCenter, dragState.drag?.pointer)
  }

  @Test
  fun `unanswered drop returns from its target`() {
    val dragState = droppedOnLegalTarget()
    val unansweredDrop = assertNotNull(dragState.unansweredDrop)

    unansweredDrop.returnUnansweredDrop(White, SquarePx)

    assertEquals(PieceDragPhase.Returning, unansweredDrop.phase)
    assertEquals(LegalTargetCenter, unansweredDrop.pointer)
  }

  private fun dragFromOrigin(): PieceDragState = PieceDragState().apply { start(Origin, Offset.Zero) }

  private fun droppedOnLegalTarget(): PieceDragState =
    dragFromOrigin().apply { end(LegalTarget, SelectedState, ::recordDrop) }

  private fun recordDrop(from: Square, to: Square) {
    drops += from to to
  }

  private companion object {
    const val SquarePx = 10f
    const val SquareSizeLift = 0f
    val White = BoardOrientation(PieceColor.WHITE)
    val Origin = Square(ChessFile.E, ChessRank.TWO)
    val LegalTarget = Square(ChessFile.E, ChessRank.FOUR)
    val IllegalTarget = Square(ChessFile.E, ChessRank.FIVE)
    val LegalTargetCenter = Offset(45f, 45f)
    val UnselectedState = BoardState(position = initialChessPosition())
    val SelectedState =
      UnselectedState.copy(
        selectedSquare = Origin,
        legalTargets = setOf(Square(ChessFile.E, ChessRank.THREE), LegalTarget),
      )
    val MovedPosition = ChessPosition.fromSnapshot(fen = Fen(""), pieces = emptyMap(), sideToMove = PieceColor.BLACK)
  }
}
