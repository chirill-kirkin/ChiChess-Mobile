package com.github.chirillkirkin.chichess.feature.game.board

import com.github.chirillkirkin.chichess.feature.game.domain.ChessPosition
import com.github.chirillkirkin.chichess.feature.game.domain.Square

data class BoardState(
  val position: ChessPosition,
  val selectedSquare: Square? = null,
  val legalTargets: Set<Square> = emptySet(),
  val checkedKingSquare: Square? = null,
)

sealed interface BoardMessage {
  data class SquareClick(val square: Square) : BoardMessage
}

fun boardUpdate(message: BoardMessage, state: BoardState): BoardState = when (message) {
  is BoardMessage.SquareClick -> {
    val clickedSquare = message.square
    val clickedPiece = state.position[clickedSquare]
    when {
      clickedSquare == state.selectedSquare -> state.copy(selectedSquare = null)

      clickedPiece?.color == state.position.sideToMove ->
        state.copy(selectedSquare = clickedSquare)

      else -> state
    }
  }
}
