package com.github.chirillkirkin.chichess.feature.game.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.round
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.github.chirillkirkin.chichess.core.designsystem.theme.ChiChessColors
import com.github.chirillkirkin.chichess.core.designsystem.theme.ChiChessTheme
import com.github.chirillkirkin.chichess.feature.game.domain.ChessFile
import com.github.chirillkirkin.chichess.feature.game.domain.ChessPiece
import com.github.chirillkirkin.chichess.feature.game.domain.ChessPosition
import com.github.chirillkirkin.chichess.feature.game.domain.ChessRank
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.PieceType
import com.github.chirillkirkin.chichess.feature.game.domain.PromotionPiece
import com.github.chirillkirkin.chichess.feature.game.domain.Square
import com.github.chirillkirkin.chichess.feature.game.domain.SquareColor
import com.github.chirillkirkin.chichess.core.designsystem.R as DesignSystemR

private const val EqualBoardSegmentWeight = 1f
private val PromotionPieceSize = 48.dp
private const val UpsideDownRotationDegrees = 180f
private const val PieceMoveDurationMillis = 200
private val PieceMoveAnimationSpec = tween<Float>(PieceMoveDurationMillis, easing = FastOutSlowInEasing)
private const val AnimationStart = 0f
private const val AnimationEnd = 1f
private const val StationaryPieceZIndex = 0f
private const val MovingPieceZIndex = 1f
private const val SquarePieceScale = 1f
private const val DraggedPieceScale = 1.5f
private const val DraggedPieceLiftInSquares = 1f
private const val DragTargetHighlightRadiusInSquares = 1f
private const val ScreenUpDirection = -1f
private const val ScreenDownDirection = 1f
private const val DropResponseFrameCount = 2

private object PromotionMenuPositionProvider : PopupPositionProvider {
  override fun calculatePosition(
    anchorBounds: IntRect,
    windowSize: IntSize,
    layoutDirection: LayoutDirection,
    popupContentSize: IntSize,
  ): IntOffset {
    val preferredX =
      when (layoutDirection) {
        LayoutDirection.Ltr -> anchorBounds.left
        LayoutDirection.Rtl -> anchorBounds.right - popupContentSize.width
      }
    val belowAnchor = anchorBounds.bottom
    val aboveAnchor = anchorBounds.top - popupContentSize.height
    val preferredY = if (belowAnchor + popupContentSize.height <= windowSize.height) belowAnchor else aboveAnchor

    return IntOffset(
      x = preferredX.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
      y = preferredY.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
    )
  }
}

@Composable
fun ChessBoard(
  state: BoardState,
  onSquareClick: (Square) -> Unit,
  onPieceDragStart: (Square) -> Unit,
  onPieceDrop: (from: Square, to: Square) -> Unit,
  onPromotionSelect: (PromotionPiece) -> Unit,
  onPromotionDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  perspective: PieceColor = PieceColor.WHITE,
  facing: BoardFacing = BoardFacing.BOTTOM,
  promotionSquare: Square? = null,
) {
  val orientation = remember(perspective) { BoardOrientation(perspective) }
  val pieceDragState = remember { PieceDragState() }
  val currentState by rememberUpdatedState(state)
  val currentOnPieceDragStart by rememberUpdatedState(onPieceDragStart)
  val currentOnPieceDrop by rememberUpdatedState(onPieceDrop)

  val pieceAnimation = rememberPieceAnimation(state.position, pieceDragState.drag)

  val isUpsideDown = { piece: ChessPiece -> facing.isUpsideDown(piece.color, perspective) }

  BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
    val boardSize = minOf(maxWidth, maxHeight)
    val squareSize = boardSize / orientation.files.size
    val squarePx = with(LocalDensity.current) { squareSize.toPx() }
    val shownDrag = pieceDragState.shownDrag(state)

    PieceDragEffects(pieceDragState, state.position, promotionSquare, orientation, squarePx)

    Box(
      modifier =
        Modifier
          .size(boardSize)
          .pieceDragGestures(
            orientation = orientation,
            onDragStart = { origin, pointer ->
              currentOnPieceDragStart(origin)
              pieceDragState.start(origin, pointer)
            },
            onDrag = pieceDragState::move,
            onDragEnd = { target -> pieceDragState.end(target, currentState, currentOnPieceDrop) },
            onDragCancel = { pieceDragState.cancel(currentState) },
          ),
    ) {
      BoardSquares(state = state, orientation = orientation, onSquareClick = onSquareClick) { square ->
        if (square == promotionSquare) {
          PromotionMenu(
            color = state.position.sideToMove,
            isUpsideDown = facing.isUpsideDown(state.position.sideToMove, perspective),
            onPieceSelect = onPromotionSelect,
            onDismiss = onPromotionDismiss,
          )
        }
      }

      if (shownDrag?.phase == PieceDragPhase.Dragging) {
        DragTargetHighlight(drag = shownDrag, orientation = orientation)
      }

      BoardPieces(
        position = state.position,
        animation = pieceAnimation,
        orientation = orientation,
        squareSize = squareSize,
        hiddenSquares = shownDrag?.hiddenSquares.orEmpty(),
        isUpsideDown = isUpsideDown,
      )

      BoardCoordinates(orientation = orientation, facing = facing)

      shownDrag?.let { drag ->
        DraggedPiece(drag, state.position, isUpsideDown, orientation, squareSize)
      }
    }
  }
}

@Composable
private fun PieceDragEffects(
  pieceDragState: PieceDragState,
  position: ChessPosition,
  promotionSquare: Square?,
  orientation: BoardOrientation,
  squarePx: Float,
) {
  SideEffect { pieceDragState.settleDrop(position, promotionSquare, orientation, squarePx) }

  val unansweredDrop = pieceDragState.unansweredDrop
  LaunchedEffect(unansweredDrop) {
    if (unansweredDrop != null) {
      // The owner answers a legal drop with a new position or a promotion request by the next frames;
      // without this the piece of an ignored drop would stay on the target square.
      repeat(DropResponseFrameCount) { withFrameNanos {} }
      unansweredDrop.returnUnansweredDrop(orientation, squarePx)
    }
  }

  val returningDrag = pieceDragState.returningDrag
  LaunchedEffect(returningDrag) {
    returningDrag?.let { pieceDragState.animateReturn(it, orientation, squarePx, PieceMoveAnimationSpec) }
  }
}

@Composable
private fun BoxScope.BoardGrid(orientation: BoardOrientation, squareContent: @Composable BoxScope.(Square) -> Unit) {
  Column(modifier = Modifier.matchParentSize()) {
    orientation.ranks.forEach { rank ->
      Row(modifier = Modifier.fillMaxWidth().weight(EqualBoardSegmentWeight)) {
        orientation.files.forEach { file ->
          Box(modifier = Modifier.fillMaxHeight().weight(EqualBoardSegmentWeight)) {
            squareContent(Square(file, rank))
          }
        }
      }
    }
  }
}

@Composable
private fun BoxScope.BoardSquares(
  state: BoardState,
  orientation: BoardOrientation,
  onSquareClick: (Square) -> Unit,
  squareContent: @Composable BoxScope.(Square) -> Unit,
) {
  val colors = ChiChessTheme.colors
  BoardGrid(orientation = orientation) { square ->
    Box(
      modifier =
        Modifier
          .matchParentSize()
          .background(square.toBoardColor(state, colors))
          .selectable(
            selected = square == state.selectedSquare,
            interactionSource = null,
            indication = null,
            onClick = { onSquareClick(square) },
          ),
    ) {
      squareContent(square)
    }
  }
}

@Composable
private fun BoxScope.BoardPieces(
  position: ChessPosition,
  animation: PieceAnimation,
  orientation: BoardOrientation,
  squareSize: Dp,
  hiddenSquares: Set<Square>,
  isUpsideDown: (ChessPiece) -> Boolean,
) {
  Box(modifier = Modifier.matchParentSize()) {
    animation.transition.vanishedPieces.forEach { (square, piece) ->
      key(square) {
        Box(
          modifier =
            Modifier
              .size(squareSize)
              .offset { orientation.squareOffset(square, squareSize.toPx()) }
              .graphicsLayer { alpha = AnimationEnd - animation.progress.value },
        ) {
          PieceImage(piece, isUpsideDown(piece))
        }
      }
    }

    orientation.ranks.forEach { rank ->
      orientation.files.forEach { file ->
        val square = Square(file, rank)
        val piece = position[square]
        if (piece != null && square !in hiddenSquares) {
          key(square) {
            val origin = animation.transition.movements[square]
            Box(
              modifier =
                Modifier
                  .size(squareSize)
                  .zIndex(if (origin != null) MovingPieceZIndex else StationaryPieceZIndex)
                  .offset {
                    val squarePx = squareSize.toPx()
                    val target = orientation.squareOffset(square, squarePx)
                    if (origin == null) {
                      target
                    } else {
                      lerp(orientation.squareOffset(origin, squarePx), target, animation.progress.value)
                    }
                  },
            ) {
              PieceImage(piece, isUpsideDown(piece))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BoxScope.BoardCoordinates(orientation: BoardOrientation, facing: BoardFacing) {
  val colors = ChiChessTheme.colors
  val leftFile = orientation.files.first()
  val rightFile = orientation.files.last()
  val topRank = orientation.ranks.first()
  val bottomRank = orientation.ranks.last()

  BoardGrid(orientation = orientation) { square ->
    BoardSquareCoordinates(
      facing = facing,
      bottomPlayerRank = square.rank.takeIf { square.file == leftFile },
      bottomPlayerFile = square.file.takeIf { square.rank == bottomRank },
      topPlayerRank = square.rank.takeIf { square.file == rightFile },
      topPlayerFile = square.file.takeIf { square.rank == topRank },
      color = square.color.toCoordinateColor(colors),
    )
  }
}

@Composable
private fun BoxScope.DragTargetHighlight(drag: PieceDrag, orientation: BoardOrientation) {
  val color = ChiChessTheme.colors.boardDragTargetOverlay
  Canvas(modifier = Modifier.matchParentSize().clipToBounds()) {
    val squarePx = size.width / orientation.files.size
    orientation.squareAt(drag.pointer, squarePx)?.let { target ->
      drawCircle(
        color = color,
        radius = DragTargetHighlightRadiusInSquares * squarePx,
        center = orientation.squareCenter(target, squarePx),
      )
    }
  }
}

@Composable
private fun DraggedPiece(
  drag: PieceDrag,
  position: ChessPosition,
  isUpsideDown: (ChessPiece) -> Boolean,
  orientation: BoardOrientation,
  squareSize: Dp,
) {
  val piece = position[drag.origin] ?: return
  val isPieceUpsideDown = isUpsideDown(piece)
  Box(
    modifier =
      Modifier
        .size(squareSize)
        .offset {
          val squarePx = squareSize.toPx()
          // The piece is raised away from its own player, so an upside-down piece moves down the screen.
          val liftDirection = if (isPieceUpsideDown) ScreenDownDirection else ScreenUpDirection
          val lift = Offset(x = 0f, y = liftDirection * DraggedPieceLiftInSquares * squarePx * drag.lift)
          val squareCenter = Offset(squarePx, squarePx) * SquareCenterFraction
          (drag.center(orientation, squarePx) + lift - squareCenter).round()
        }.graphicsLayer {
          val scale = lerp(SquarePieceScale, DraggedPieceScale, drag.lift)
          scaleX = scale
          scaleY = scale
        },
  ) {
    PieceImage(piece, isPieceUpsideDown)
  }
}

@Stable
private class PieceAnimation(val transition: PieceTransition) {
  val progress =
    Animatable(
      if (transition.movements.isEmpty() && transition.vanishedPieces.isEmpty()) AnimationEnd else AnimationStart,
    )
}

@Composable
private fun rememberPieceAnimation(position: ChessPosition, pieceDrag: PieceDrag?): PieceAnimation {
  var previousPosition by remember { mutableStateOf(position) }
  val animation =
    remember(position) {
      val transition = pieceTransition(previousPosition, position)
      val droppedTarget = pieceDrag?.droppedTarget
      // A dropped piece is already on its destination, so only the rest of the move is animated.
      PieceAnimation(
        if (droppedTarget == null) transition else transition.withoutMovement(pieceDrag.origin, droppedTarget),
      )
    }
  SideEffect { previousPosition = position }
  LaunchedEffect(animation) {
    animation.progress.animateTo(AnimationEnd, PieceMoveAnimationSpec)
  }
  return animation
}

@Composable
private fun PromotionMenu(
  color: PieceColor,
  isUpsideDown: Boolean,
  onPieceSelect: (PromotionPiece) -> Unit,
  onDismiss: () -> Unit,
) {
  Popup(
    popupPositionProvider = PromotionMenuPositionProvider,
    onDismissRequest = onDismiss,
    properties = PopupProperties(focusable = true),
  ) {
    Surface(
      shape = MenuDefaults.shape,
      color = MenuDefaults.containerColor,
      shadowElevation = MenuDefaults.ShadowElevation,
    ) {
      Column {
        PromotionPiece.entries.forEach { promotionPiece ->
          Box(
            modifier =
              Modifier
                .size(PromotionPieceSize)
                .clickable { onPieceSelect(promotionPiece) },
          ) {
            PieceImage(ChessPiece(color, promotionPiece.toPieceType()), isUpsideDown)
          }
        }
      }
    }
  }
}

@Composable
private fun PieceImage(piece: ChessPiece, isUpsideDown: Boolean) {
  Image(
    painter = painterResource(piece.drawableResource()),
    contentDescription = null,
    modifier = Modifier.fillMaxSize().then(if (isUpsideDown) Modifier.rotate(UpsideDownRotationDegrees) else Modifier),
    contentScale = ContentScale.Fit,
  )
}

@Composable
private fun BoxScope.BoardSquareCoordinates(
  facing: BoardFacing,
  bottomPlayerRank: ChessRank?,
  bottomPlayerFile: ChessFile?,
  topPlayerRank: ChessRank?,
  topPlayerFile: ChessFile?,
  color: Color,
) {
  if (facing.showsBottomCoordinates) {
    SquareCoordinates(rank = bottomPlayerRank, file = bottomPlayerFile, color = color)
  }

  if (facing.showsTopCoordinates) {
    Box(modifier = Modifier.matchParentSize().rotate(UpsideDownRotationDegrees)) {
      SquareCoordinates(rank = topPlayerRank, file = topPlayerFile, color = color)
    }
  }
}

@Composable
private fun BoxScope.SquareCoordinates(rank: ChessRank?, file: ChessFile?, color: Color) {
  if (rank != null) {
    SquareCoordinate(text = rank.notation.toString(), alignment = Alignment.TopStart, color = color)
  }
  if (file != null) {
    SquareCoordinate(text = file.notation.toString(), alignment = Alignment.BottomStart, color = color)
  }
}

@Composable
private fun BoxScope.SquareCoordinate(text: String, alignment: Alignment, color: Color) {
  Text(
    text = text,
    modifier =
      Modifier
        .align(alignment)
        .padding(start = ChiChessTheme.spacing.boardCoordinateInset),
    color = color,
    style = ChiChessTheme.typography.boardCoordinate,
  )
}

private val BoardFacing.showsBottomCoordinates: Boolean
  get() = when (this) {
    BoardFacing.BOTTOM, BoardFacing.FACE_TO_FACE -> true
    BoardFacing.TOP -> false
  }

private val BoardFacing.showsTopCoordinates: Boolean
  get() = when (this) {
    BoardFacing.TOP, BoardFacing.FACE_TO_FACE -> true
    BoardFacing.BOTTOM -> false
  }

private fun BoardFacing.isUpsideDown(color: PieceColor, perspective: PieceColor): Boolean = when (this) {
  BoardFacing.BOTTOM -> false
  BoardFacing.TOP -> true
  BoardFacing.FACE_TO_FACE -> color != perspective
}

internal fun PromotionPiece.toPieceType(): PieceType = when (this) {
  PromotionPiece.QUEEN -> PieceType.QUEEN
  PromotionPiece.ROOK -> PieceType.ROOK
  PromotionPiece.BISHOP -> PieceType.BISHOP
  PromotionPiece.KNIGHT -> PieceType.KNIGHT
}

private fun Square.toBoardColor(state: BoardState, colors: ChiChessColors): Color = color
  .toBoardColor(colors)
  .withHighlight(
    isCheckedKing = this == state.checkedKingSquare,
    isSelected = this == state.selectedSquare,
    isLegalTarget = this in state.legalTargets,
    colors = colors,
  )

private fun Color.withHighlight(
  isCheckedKing: Boolean,
  isSelected: Boolean,
  isLegalTarget: Boolean,
  colors: ChiChessColors,
): Color = when {
  isCheckedKing -> colors.boardCheckedKingOverlay.compositeOver(this)
  isSelected -> colors.boardSelectionOverlay.compositeOver(this)
  isLegalTarget -> colors.boardLegalTargetOverlay.compositeOver(this)
  else -> this
}

private fun SquareColor.toBoardColor(colors: ChiChessColors): Color = when (this) {
  SquareColor.LIGHT -> colors.lightBoardSquare
  SquareColor.DARK -> colors.darkBoardSquare
}

private fun SquareColor.toCoordinateColor(colors: ChiChessColors): Color = when (this) {
  SquareColor.LIGHT -> colors.darkBoardSquare
  SquareColor.DARK -> colors.lightBoardSquare
}

private fun ChessPiece.drawableResource(): Int = when (color) {
  PieceColor.WHITE ->
    when (type) {
      PieceType.KING -> DesignSystemR.drawable.ic_piece_white_king
      PieceType.QUEEN -> DesignSystemR.drawable.ic_piece_white_queen
      PieceType.ROOK -> DesignSystemR.drawable.ic_piece_white_rook
      PieceType.BISHOP -> DesignSystemR.drawable.ic_piece_white_bishop
      PieceType.KNIGHT -> DesignSystemR.drawable.ic_piece_white_knight
      PieceType.PAWN -> DesignSystemR.drawable.ic_piece_white_pawn
    }

  PieceColor.BLACK ->
    when (type) {
      PieceType.KING -> DesignSystemR.drawable.ic_piece_black_king
      PieceType.QUEEN -> DesignSystemR.drawable.ic_piece_black_queen
      PieceType.ROOK -> DesignSystemR.drawable.ic_piece_black_rook
      PieceType.BISHOP -> DesignSystemR.drawable.ic_piece_black_bishop
      PieceType.KNIGHT -> DesignSystemR.drawable.ic_piece_black_knight
      PieceType.PAWN -> DesignSystemR.drawable.ic_piece_black_pawn
    }
}
