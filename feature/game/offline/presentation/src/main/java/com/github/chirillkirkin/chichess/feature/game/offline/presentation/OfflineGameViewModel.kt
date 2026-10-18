package com.github.chirillkirkin.chichess.feature.game.offline.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.chirillkirkin.chichess.feature.game.board.BoardMessage
import com.github.chirillkirkin.chichess.feature.game.board.BoardState
import com.github.chirillkirkin.chichess.feature.game.board.boardUpdate
import com.github.chirillkirkin.chichess.feature.game.domain.ChessGameEngine
import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.DrawReason
import com.github.chirillkirkin.chichess.feature.game.domain.GameStatus
import com.github.chirillkirkin.chichess.feature.game.domain.MoveApplicationResult
import com.github.chirillkirkin.chichess.feature.game.domain.MoveRejectionReason
import com.github.chirillkirkin.chichess.feature.game.domain.PromotionPiece
import com.github.chirillkirkin.chichess.feature.game.domain.initialChessPosition
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineBoardLayout
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineGameRepository
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineGameSnapshot
import com.github.chirillkirkin.mvu.CommandExecutor
import com.github.chirillkirkin.mvu.MVU
import com.github.chirillkirkin.mvu.MVUStore
import com.github.chirillkirkin.mvu.Update
import com.github.chirillkirkin.mvu.update
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class OfflineGameState(
  val board: BoardState = BoardState(position = initialChessPosition()),
  val pendingPromotion: ChessMove? = null,
  val gameStatus: GameStatus = GameStatus.Ongoing,
  val claimableDrawReason: DrawReason? = null,
  val boardLayout: OfflineBoardLayout = OfflineBoardLayout.STANDARD,
  val isLoading: Boolean = false,
)

sealed interface OfflineGameMessage {
  data class Board(val message: BoardMessage) : OfflineGameMessage

  data class PromotionSelected(val piece: PromotionPiece) : OfflineGameMessage

  data object PromotionDismissed : OfflineGameMessage

  data object ClaimDraw : OfflineGameMessage

  data class BoardLayoutSelected(val layout: OfflineBoardLayout) : OfflineGameMessage

  data object NewGame : OfflineGameMessage

  data class GameLoaded(val snapshot: OfflineGameSnapshot?) : OfflineGameMessage
}

sealed interface OfflineGameCommand {
  data object LoadGame : OfflineGameCommand

  data class StartNewGame(val boardLayout: OfflineBoardLayout) : OfflineGameCommand

  data class AppendMove(val halfMoveIndex: Int, val move: ChessMove) : OfflineGameCommand

  data class UpdateBoardLayout(val boardLayout: OfflineBoardLayout) : OfflineGameCommand

  data class UpdateDrawReason(val drawReason: DrawReason) : OfflineGameCommand
}

internal fun offlineGameUpdate(
  gameEngine: ChessGameEngine,
): Update<OfflineGameMessage, OfflineGameState, OfflineGameCommand> = update { message, state ->
  if (state.isLoading && message !is OfflineGameMessage.GameLoaded) return@update

  when (message) {
    is OfflineGameMessage.Board ->
      if (state.pendingPromotion == null && state.gameStatus == GameStatus.Ongoing) {
        val updatedState = state.updateBoard(message.message, gameEngine)
        state(updatedState)
        commands(updatedState.appendedMoveCommands(state))
      }

    is OfflineGameMessage.PromotionSelected -> {
      val updatedState = state.selectPromotion(message.piece, gameEngine)
      state(updatedState)
      commands(updatedState.appendedMoveCommands(state))
    }

    OfflineGameMessage.PromotionDismissed -> state(state.copy(pendingPromotion = null))

    OfflineGameMessage.ClaimDraw -> {
      val drawReason = state.claimableDrawReason
      if (state.gameStatus == GameStatus.Ongoing && drawReason != null) {
        state(state.copy(gameStatus = GameStatus.Draw(drawReason), claimableDrawReason = null))
        command(OfflineGameCommand.UpdateDrawReason(drawReason))
      }
    }

    is OfflineGameMessage.BoardLayoutSelected -> {
      state(state.copy(boardLayout = message.layout))
      command(OfflineGameCommand.UpdateBoardLayout(message.layout))
    }

    OfflineGameMessage.NewGame -> {
      state(OfflineGameState(boardLayout = state.boardLayout))
      command(OfflineGameCommand.StartNewGame(state.boardLayout))
    }

    is OfflineGameMessage.GameLoaded -> {
      val snapshot = message.snapshot
      if (snapshot == null) {
        state(state.copy(isLoading = false))
        command(OfflineGameCommand.StartNewGame(state.boardLayout))
      } else {
        state(state.restoredFrom(snapshot, gameEngine))
      }
    }
  }
}

private fun OfflineGameState.appendedMoveCommands(previousState: OfflineGameState): List<OfflineGameCommand> {
  val moveHistory = board.position.moveHistory
  return if (moveHistory.size > previousState.board.position.moveHistory.size) {
    listOf(OfflineGameCommand.AppendMove(halfMoveIndex = moveHistory.lastIndex, move = moveHistory.last()))
  } else {
    emptyList()
  }
}

private fun OfflineGameState.selectPromotion(piece: PromotionPiece, gameEngine: ChessGameEngine): OfflineGameState {
  val move = pendingPromotion ?: return this
  return applyMove(move.copy(promotion = piece), gameEngine)
}

private fun OfflineGameState.updateBoard(message: BoardMessage, gameEngine: ChessGameEngine): OfflineGameState =
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

private fun OfflineGameState.applyMove(move: ChessMove, gameEngine: ChessGameEngine): OfflineGameState =
  when (val result = gameEngine.applyMove(board.position, move)) {
    is MoveApplicationResult.Applied -> {
      val status = gameEngine.gameStatus(result.position)
      copy(
        board =
          board.copy(
            position = result.position,
            selectedSquare = null,
            legalTargets = emptySet(),
            checkedKingSquare = gameEngine.checkedKingSquare(result.position),
          ),
        pendingPromotion = null,
        gameStatus = status,
        claimableDrawReason =
          if (status == GameStatus.Ongoing) gameEngine.claimableDrawReason(result.position) else null,
      )
    }

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
    updatedBoard.selectedSquare
      ?.let { selectedSquare ->
        gameEngine
          .legalMoves(updatedBoard.position)
          .asSequence()
          .filter { it.from == selectedSquare }
          .map { it.to }
          .toSet()
      }.orEmpty()

  return copy(board = updatedBoard.copy(legalTargets = legalTargets))
}

private fun OfflineGameState.restoredFrom(
  snapshot: OfflineGameSnapshot,
  gameEngine: ChessGameEngine,
): OfflineGameState {
  val position =
    snapshot.moves.fold(initialChessPosition()) { position, move ->
      val result = gameEngine.applyMove(position, move)
      check(result is MoveApplicationResult.Applied) { "Cannot replay the stored move $move" }
      result.position
    }
  val status = snapshot.drawReason?.let(GameStatus::Draw) ?: gameEngine.gameStatus(position)
  return copy(
    board = BoardState(position = position, checkedKingSquare = gameEngine.checkedKingSquare(position)),
    gameStatus = status,
    claimableDrawReason =
      if (status == GameStatus.Ongoing) gameEngine.claimableDrawReason(position) else null,
    boardLayout = snapshot.boardLayout,
    isLoading = false,
  )
}

internal fun offlineGameCommandExecutor(
  repository: OfflineGameRepository,
): CommandExecutor<OfflineGameCommand, OfflineGameMessage> = { command ->
  when (command) {
    OfflineGameCommand.LoadGame -> flow { emit(OfflineGameMessage.GameLoaded(repository.load())) }
    is OfflineGameCommand.StartNewGame -> persist { repository.startNewGame(command.boardLayout) }
    is OfflineGameCommand.AppendMove -> persist { repository.appendMove(command.halfMoveIndex, command.move) }
    is OfflineGameCommand.UpdateBoardLayout -> persist { repository.updateBoardLayout(command.boardLayout) }
    is OfflineGameCommand.UpdateDrawReason -> persist { repository.updateDrawReason(command.drawReason) }
  }
}

// A write must finish even if the screen is closed right after the move that produced it.
private fun persist(write: suspend () -> Unit): Flow<OfflineGameMessage> = flow {
  withContext(NonCancellable) { write() }
}

@ViewModelScoped
class OfflineGameStore @Inject constructor(gameEngine: ChessGameEngine, repository: OfflineGameRepository) :
  MVUStore<OfflineGameMessage, OfflineGameState, OfflineGameCommand>(
    initialState = OfflineGameState(isLoading = true),
    update = offlineGameUpdate(gameEngine),
    commandExecutor = offlineGameCommandExecutor(repository),
    initialCommands = listOf(OfflineGameCommand.LoadGame),
    // Writes depend on each other (a new game replaces the moves of the previous one), so they run in order.
    concurrency = 1,
  )

@HiltViewModel
class OfflineGameViewModel @Inject constructor(store: OfflineGameStore) :
  ViewModel(),
  MVU<OfflineGameMessage, OfflineGameState, OfflineGameCommand> by store {
    init {
      launchIn(viewModelScope)
    }
  }
