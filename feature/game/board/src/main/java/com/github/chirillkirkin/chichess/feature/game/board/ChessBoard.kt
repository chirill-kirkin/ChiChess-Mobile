package com.github.chirillkirkin.chichess.feature.game.board

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.github.chirillkirkin.chichess.core.designsystem.theme.ChiChessColors
import com.github.chirillkirkin.chichess.core.designsystem.theme.ChiChessTheme
import com.github.chirillkirkin.chichess.feature.game.domain.ChessFile
import com.github.chirillkirkin.chichess.feature.game.domain.ChessPiece
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
  modifier: Modifier = Modifier,
  perspective: PieceColor = PieceColor.WHITE,
  facing: BoardFacing = BoardFacing.BOTTOM,
  promotionSquare: Square? = null,
  onPromotionSelect: (PromotionPiece) -> Unit = {},
  onPromotionDismiss: () -> Unit = {},
) {
  val colors = ChiChessTheme.colors
  val ranks = if (perspective == PieceColor.WHITE) ChessRank.entries.asReversed() else ChessRank.entries
  val files = if (perspective == PieceColor.WHITE) ChessFile.entries else ChessFile.entries.asReversed()
  val leftFile = files.first()
  val rightFile = files.last()
  val topRank = ranks.first()
  val bottomRank = ranks.last()

  BoxWithConstraints(
    modifier = modifier,
    contentAlignment = Alignment.Center,
  ) {
    val boardSize = minOf(maxWidth, maxHeight)

    Column(modifier = Modifier.size(boardSize)) {
      ranks.forEach { rank ->
        Row(modifier = Modifier.fillMaxWidth().weight(EqualBoardSegmentWeight)) {
          files.forEach { file ->
            val square = Square(file, rank)
            val isSelected = square == state.selectedSquare
            val squareColor = square.toBoardColor(state, colors)
            val piece = state.position[square]

            Box(
              modifier =
                Modifier
                  .fillMaxHeight()
                  .weight(EqualBoardSegmentWeight)
                  .background(squareColor)
                  .selectable(
                    selected = isSelected,
                    interactionSource = null,
                    indication = null,
                    onClick = { onSquareClick(square) },
                  ),
              contentAlignment = Alignment.Center,
            ) {
              if (piece != null) PieceImage(piece, isUpsideDown = facing.isUpsideDown(piece.color, perspective))

              BoardSquareCoordinates(
                facing = facing,
                bottomPlayerRank = rank.takeIf { file == leftFile },
                bottomPlayerFile = file.takeIf { rank == bottomRank },
                topPlayerRank = rank.takeIf { file == rightFile },
                topPlayerFile = file.takeIf { rank == topRank },
                color = square.color.toCoordinateColor(colors),
              )

              if (square == promotionSquare) {
                PromotionMenu(
                  color = state.position.sideToMove,
                  isUpsideDown = facing.isUpsideDown(state.position.sideToMove, perspective),
                  onPieceSelect = onPromotionSelect,
                  onDismiss = onPromotionDismiss,
                )
              }
            }
          }
        }
      }
    }
  }
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

private fun PromotionPiece.toPieceType(): PieceType = when (this) {
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
