package com.github.chirillkirkin.chichess.feature.game.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.chirillkirkin.chichess.feature.game.board.BoardMessage
import com.github.chirillkirkin.chichess.feature.game.board.BoardState
import com.github.chirillkirkin.chichess.feature.game.board.boardUpdate
import com.github.chirillkirkin.chichess.feature.game.domain.ChessGameEngine
import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.MoveApplicationResult
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
)

sealed interface OfflineGameMessage {
  data class Board(
    val message: BoardMessage,
  ) : OfflineGameMessage
}

internal fun offlineGameUpdate(
  gameEngine: ChessGameEngine,
): Update<OfflineGameMessage, OfflineGameState, Nothing> =
  update { message, _ ->
    when (message) {
      is OfflineGameMessage.Board ->
        state {
          updateBoard(message.message, gameEngine)
        }
    }
  }

private fun OfflineGameState.updateBoard(
  message: BoardMessage,
  gameEngine: ChessGameEngine,
): OfflineGameState =
  when (message) {
    is BoardMessage.SquareClick -> {
      val selectedSquare = board.selectedSquare
      if (selectedSquare == null || selectedSquare == message.square) {
        copy(board = boardUpdate(message, board))
      } else if (board.position[selectedSquare]?.color == board.position[message.square]?.color) {
        copy(board = boardUpdate(message, board))
      } else {
        val move = ChessMove(from = selectedSquare, to = message.square)
        when (val result = gameEngine.applyMove(board.position, move)) {
          is MoveApplicationResult.Applied ->
            copy(
              board =
                board.copy(
                  position = result.position,
                  selectedSquare = null,
                ),
            )

          is MoveApplicationResult.Rejected -> this
        }
      }
    }
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
