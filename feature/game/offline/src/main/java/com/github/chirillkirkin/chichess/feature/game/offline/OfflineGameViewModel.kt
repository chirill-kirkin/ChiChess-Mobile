package com.github.chirillkirkin.chichess.feature.game.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.chirillkirkin.chichess.feature.game.board.BoardMessage
import com.github.chirillkirkin.chichess.feature.game.board.BoardState
import com.github.chirillkirkin.chichess.feature.game.board.boardUpdate
import com.github.chirillkirkin.chichess.feature.game.domain.ChessGameEngine
import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.GameStatus
import com.github.chirillkirkin.chichess.feature.game.domain.MoveApplicationResult
import com.github.chirillkirkin.chichess.feature.game.domain.MoveRejectionReason
import com.github.chirillkirkin.chichess.feature.game.domain.PromotionPiece
import com.github.chirillkirkin.chichess.feature.game.domain.initialChessPosition
import com.github.chirillkirkin.mvu.MVU
import com.github.chirillkirkin.mvu.MVUStore
import com.github.chirillkirkin.mvu.Update
import com.github.chirillkirkin.mvu.update
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.emptyFlow

data class OfflineGameState(
  val board: BoardState = BoardState(position = initialChessPosition()),
  val pendingPromotion: ChessMove? = null,
  val gameStatus: GameStatus = GameStatus.Ongoing,
)

sealed interface OfflineGameMessage {
  data class Board(
    val message: BoardMessage,
  ) : OfflineGameMessage

  data class PromotionSelected(
    val piece: PromotionPiece,
  ) : OfflineGameMessage

  data object PromotionDismissed : OfflineGameMessage
}

internal fun offlineGameUpdate(
  gameEngine: ChessGameEngine,
): Update<OfflineGameMessage, OfflineGameState, Nothing> =
  update { message, _ ->
    when (message) {
      is OfflineGameMessage.Board ->
        state {
          if (pendingPromotion == null && gameStatus == GameStatus.Ongoing) {
            updateBoard(message.message, gameEngine)
          } else {
            this
          }
        }

      is OfflineGameMessage.PromotionSelected ->
        state {
          selectPromotion(message.piece, gameEngine)
        }

      OfflineGameMessage.PromotionDismissed ->
        state {
          copy(pendingPromotion = null)
        }
    }
  }

private fun OfflineGameState.selectPromotion(
  piece: PromotionPiece,
  gameEngine: ChessGameEngine,
): OfflineGameState {
  val move = pendingPromotion ?: return this
  return applyMove(move.copy(promotion = piece), gameEngine)
}

private fun OfflineGameState.updateBoard(
  message: BoardMessage,
  gameEngine: ChessGameEngine,
): OfflineGameState =
  when (message) {
    is BoardMessage.SquareClick -> {
      val selectedSquare = board.selectedSquare
      if (
        selectedSquare == null ||
        selectedSquare == message.square ||
        board.position[selectedSquare]?.color == board.position[message.square]?.color
      ) {
        selectSquare(message, gameEngine)
      } else {
        applyMove(ChessMove(from = selectedSquare, to = message.square), gameEngine)
      }
    }
  }

private fun OfflineGameState.applyMove(
  move: ChessMove,
  gameEngine: ChessGameEngine,
): OfflineGameState =
  when (val result = gameEngine.applyMove(board.position, move)) {
    is MoveApplicationResult.Applied ->
      copy(
        board =
          board.copy(
            position = result.position,
            selectedSquare = null,
            legalTargets = emptySet(),
            checkedKingSquare = gameEngine.checkedKingSquare(result.position),
          ),
        pendingPromotion = null,
        gameStatus = gameEngine.gameStatus(result.position),
      )

    is MoveApplicationResult.Rejected ->
      if (result.reason == MoveRejectionReason.PROMOTION_REQUIRED && move.promotion == null) {
        copy(pendingPromotion = move)
      } else {
        this
      }
  }

private fun OfflineGameState.selectSquare(
  message: BoardMessage.SquareClick,
  gameEngine: ChessGameEngine,
): OfflineGameState {
  val updatedBoard = boardUpdate(message, board)
  val legalTargets =
    updatedBoard.selectedSquare?.let { selectedSquare ->
      gameEngine
        .legalMoves(updatedBoard.position)
        .asSequence()
        .filter { it.from == selectedSquare }
        .map { it.to }
        .toSet()
    } ?: emptySet()

  return copy(board = updatedBoard.copy(legalTargets = legalTargets))
}

@ViewModelScoped
class OfflineGameStore @Inject constructor(
  gameEngine: ChessGameEngine,
) :
  MVUStore<OfflineGameMessage, OfflineGameState, Nothing>(
    initialState = OfflineGameState(),
    update = offlineGameUpdate(gameEngine),
    commandExecutor = { emptyFlow() },
  )

@HiltViewModel
class OfflineGameViewModel @Inject constructor(
  store: OfflineGameStore,
) : ViewModel(), MVU<OfflineGameMessage, OfflineGameState, Nothing> by store {
  init {
    launchIn(viewModelScope)
  }
}
