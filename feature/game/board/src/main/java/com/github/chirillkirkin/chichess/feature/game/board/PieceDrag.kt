package com.github.chirillkirkin.chichess.feature.game.board

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.lerp
import com.github.chirillkirkin.chichess.feature.game.domain.ChessPosition
import com.github.chirillkirkin.chichess.feature.game.domain.Square

private const val NoLift = 0f
private const val FullLift = 1f
private const val ReturnStart = 0f
private const val ReturnEnd = 1f

internal sealed interface PieceDragPhase {
  data object Dragging : PieceDragPhase

  data class Dropped(val target: Square, val position: ChessPosition, val isAwaitingPromotion: Boolean = false) :
    PieceDragPhase

  data object Returning : PieceDragPhase
}

@Stable
internal class PieceDrag(val origin: Square, pointer: Offset) {
  var pointer by mutableStateOf(pointer)

  /** Interpolates the dragged piece between its square size ([NoLift]) and its enlarged, raised look. */
  var lift by mutableFloatStateOf(FullLift)

  var phase: PieceDragPhase by mutableStateOf(PieceDragPhase.Dragging)

  val droppedTarget: Square?
    get() = (phase as? PieceDragPhase.Dropped)?.target

  val hiddenSquares: Set<Square>
    get() = setOfNotNull(origin, droppedTarget)

  fun isShown(state: BoardState): Boolean = when (val phase = phase) {
    PieceDragPhase.Dragging -> state.selectedSquare == origin
    is PieceDragPhase.Dropped -> state.position == phase.position
    PieceDragPhase.Returning -> true
  }

  fun center(orientation: BoardOrientation, squarePx: Float): Offset =
    droppedTarget?.let { orientation.squareCenter(it, squarePx) } ?: pointer

  /** Returns `false` when the selection never accepted the drag, so it must be discarded. */
  fun end(target: Square?, state: BoardState, onPieceDrop: (from: Square, to: Square) -> Unit): Boolean {
    if (!isShown(state)) return false
    when {
      target == null || target == origin -> phase = PieceDragPhase.Returning

      target in state.legalTargets -> {
        lift = NoLift
        phase = PieceDragPhase.Dropped(target, state.position)
        onPieceDrop(origin, target)
      }

      else -> {
        phase = PieceDragPhase.Returning
        onPieceDrop(origin, target)
      }
    }
    return true
  }

  fun cancel(state: BoardState): Boolean {
    if (!isShown(state)) return false
    phase = PieceDragPhase.Returning
    return true
  }

  /** Returns `true` once the dropped move has been applied and the drag is over. */
  fun settleDrop(
    position: ChessPosition,
    promotionSquare: Square?,
    orientation: BoardOrientation,
    squarePx: Float,
  ): Boolean {
    val dropped = phase as? PieceDragPhase.Dropped ?: return false
    when {
      position != dropped.position -> return true
      promotionSquare == dropped.target -> phase = dropped.copy(isAwaitingPromotion = true)
      dropped.isAwaitingPromotion -> returnFrom(dropped.target, orientation, squarePx)
    }
    return false
  }

  fun returnUnansweredDrop(orientation: BoardOrientation, squarePx: Float) {
    val dropped = phase as? PieceDragPhase.Dropped ?: return
    if (!dropped.isAwaitingPromotion) returnFrom(dropped.target, orientation, squarePx)
  }

  private fun returnFrom(target: Square, orientation: BoardOrientation, squarePx: Float) {
    pointer = orientation.squareCenter(target, squarePx)
    phase = PieceDragPhase.Returning
  }

  suspend fun animateReturn(target: Offset, animationSpec: AnimationSpec<Float>) {
    val startPointer = pointer
    val startLift = lift
    animate(ReturnStart, ReturnEnd, animationSpec = animationSpec) { fraction, _ ->
      pointer = lerp(startPointer, target, fraction)
      lift = lerp(startLift, NoLift, fraction)
    }
  }
}

@Stable
internal class PieceDragState {
  var drag: PieceDrag? by mutableStateOf(null)
    private set

  val returningDrag: PieceDrag?
    get() = drag?.takeIf { it.phase == PieceDragPhase.Returning }

  val unansweredDrop: PieceDrag?
    get() = drag?.takeIf { (it.phase as? PieceDragPhase.Dropped)?.isAwaitingPromotion == false }

  fun shownDrag(state: BoardState): PieceDrag? = drag?.takeIf { it.isShown(state) }

  fun start(origin: Square, pointer: Offset) {
    drag = PieceDrag(origin, pointer)
  }

  fun move(pointer: Offset) {
    drag?.pointer = pointer
  }

  fun end(target: Square?, state: BoardState, onPieceDrop: (from: Square, to: Square) -> Unit) {
    if (drag?.end(target, state, onPieceDrop) == false) drag = null
  }

  fun cancel(state: BoardState) {
    if (drag?.cancel(state) == false) drag = null
  }

  fun settleDrop(position: ChessPosition, promotionSquare: Square?, orientation: BoardOrientation, squarePx: Float) {
    if (drag?.settleDrop(position, promotionSquare, orientation, squarePx) == true) drag = null
  }

  suspend fun animateReturn(
    returningDrag: PieceDrag,
    orientation: BoardOrientation,
    squarePx: Float,
    animationSpec: AnimationSpec<Float>,
  ) {
    returningDrag.animateReturn(orientation.squareCenter(returningDrag.origin, squarePx), animationSpec)
    drag = null
  }
}

internal fun Modifier.pieceDragGestures(
  orientation: BoardOrientation,
  onDragStart: (origin: Square, pointer: Offset) -> Unit,
  onDrag: (pointer: Offset) -> Unit,
  onDragEnd: (target: Square?) -> Unit,
  onDragCancel: () -> Unit,
): Modifier = pointerInput(orientation) {
  awaitEachGesture {
    val squarePx = size.width.toFloat() / orientation.files.size
    val down = awaitFirstDown(requireUnconsumed = false)
    val origin = orientation.squareAt(down.position, squarePx) ?: return@awaitEachGesture
    val slopChange = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
      ?: return@awaitEachGesture

    var pointer = slopChange.position
    onDragStart(origin, pointer)
    val isCompleted =
      drag(slopChange.id) { change ->
        pointer = change.position
        onDrag(pointer)
        change.consume()
      }
    if (isCompleted) onDragEnd(orientation.squareAt(pointer, squarePx)) else onDragCancel()
  }
}
