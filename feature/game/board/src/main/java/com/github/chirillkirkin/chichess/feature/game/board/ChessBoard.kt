package com.github.chirillkirkin.chichess.feature.game.board

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
  promotionSquare: Square? = null,
  onPromotionSelected: (PromotionPiece) -> Unit = {},
  onPromotionDismissed: () -> Unit = {},
) {
  val boardDescription = stringResource(R.string.initial_chess_board_description)
  val colors = ChiChessTheme.colors

  BoxWithConstraints(
    modifier = modifier,
    contentAlignment = Alignment.Center,
  ) {
    val boardSize = minOf(maxWidth, maxHeight)

    Column(
      modifier =
        Modifier
          .size(boardSize)
          .semantics {
            contentDescription = boardDescription
          },
    ) {
      ChessRank.entries.asReversed().forEach { rank ->
        Row(modifier = Modifier.fillMaxWidth().weight(EqualBoardSegmentWeight)) {
          ChessFile.entries.forEach { file ->
            val square = Square(file, rank)
            val isSelected = square == state.selectedSquare
            val isLegalTarget = square in state.legalTargets
            val squareColor = square.color.toBoardColor(colors).withHighlight(isSelected, isLegalTarget, colors)
            val coordinateColor = square.color.toCoordinateColor(colors)
            val squareDescription =
              stringResource(
                R.string.chess_square_description,
                file.notation.toString(),
                rank.notation,
              )
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
                  )
                  .semantics {
                    contentDescription = squareDescription
                  },
              contentAlignment = Alignment.Center,
            ) {
              if (piece != null) {
                Image(
                  painter = painterResource(piece.drawableResource()),
                  contentDescription = null,
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Fit,
                )
              }

              if (file == ChessFile.A) {
                Text(
                  text = rank.notation.toString(),
                  modifier =
                    Modifier
                      .align(Alignment.TopStart)
                      .padding(start = ChiChessTheme.spacing.boardCoordinateInset),
                  color = coordinateColor,
                  style = ChiChessTheme.typography.boardCoordinate,
                )
              }

              if (rank == ChessRank.ONE) {
                Text(
                  text = file.notation.toString(),
                  modifier =
                    Modifier
                      .align(Alignment.BottomStart)
                      .padding(start = ChiChessTheme.spacing.boardCoordinateInset),
                  color = coordinateColor,
                  style = ChiChessTheme.typography.boardCoordinate,
                )
              }

              if (square == promotionSquare) {
                PromotionMenu(
                  color = state.position.sideToMove,
                  onPieceSelected = onPromotionSelected,
                  onDismiss = onPromotionDismissed,
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
  onPieceSelected: (PromotionPiece) -> Unit,
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
                .clickable { onPieceSelected(promotionPiece) },
          ) {
            Image(
              painter = painterResource(ChessPiece(color, promotionPiece.toPieceType()).drawableResource()),
              contentDescription = null,
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Fit,
            )
          }
        }
      }
    }
  }
}

private fun PromotionPiece.toPieceType(): PieceType =
  when (this) {
    PromotionPiece.QUEEN -> PieceType.QUEEN
    PromotionPiece.ROOK -> PieceType.ROOK
    PromotionPiece.BISHOP -> PieceType.BISHOP
    PromotionPiece.KNIGHT -> PieceType.KNIGHT
  }

private fun Color.withHighlight(
  isSelected: Boolean,
  isLegalTarget: Boolean,
  colors: ChiChessColors,
): Color =
  when {
    isSelected -> colors.boardSelectionOverlay.compositeOver(this)
    isLegalTarget -> colors.boardLegalTargetOverlay.compositeOver(this)
    else -> this
  }

private fun SquareColor.toBoardColor(colors: ChiChessColors): Color =
  when (this) {
    SquareColor.LIGHT -> colors.lightBoardSquare
    SquareColor.DARK -> colors.darkBoardSquare
  }

private fun SquareColor.toCoordinateColor(colors: ChiChessColors): Color =
  when (this) {
    SquareColor.LIGHT -> colors.darkBoardSquare
    SquareColor.DARK -> colors.lightBoardSquare
  }

private fun ChessPiece.drawableResource(): Int =
  when (color) {
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
