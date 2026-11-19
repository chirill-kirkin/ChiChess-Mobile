package com.github.chirillkirkin.chichess.feature.game.offline.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.chirillkirkin.chichess.feature.game.board.BoardMessage
import com.github.chirillkirkin.chichess.feature.game.board.BoardState
import com.github.chirillkirkin.chichess.feature.game.board.boardUpdate
import com.github.chirillkirkin.chichess.feature.game.domain.ChessClock
import com.github.chirillkirkin.chichess.feature.game.domain.ChessGameEngine
import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.DrawReason
import com.github.chirillkirkin.chichess.feature.game.domain.GameStatus
import com.github.chirillkirkin.chichess.feature.game.domain.MoveApplicationResult
import com.github.chirillkirkin.chichess.feature.game.domain.MoveRejectionReason
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.PromotionPiece
import com.github.chirillkirkin.chichess.feature.game.domain.TimeControl
import com.github.chirillkirkin.chichess.feature.game.domain.initialChessPosition
import com.github.chirillkirkin.chichess.feature.game.domain.timeoutStatus
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineBoardLayout
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineClock
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineGameRepository
import com.github.chirillkirkin.chichess.feature.game.offline.domain.OfflineGameSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import io.github.chirillkirkin.chelm.CommandExecutor
import io.github.chirillkirkin.chelm.MVU
import io.github.chirillkirkin.chelm.MVUStore
import io.github.chirillkirkin.chelm.Subscription
import io.github.chirillkirkin.chelm.Update
import io.github.chirillkirkin.chelm.UpdateDsl
import io.github.chirillkirkin.chelm.update
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.ComparableTimeMark
import kotlin.time.TimeSource

data class OfflineGameState(
  val board: BoardState = BoardState(position = initialChessPosition()),
  val pendingPromotion: ChessMove? = null,
  val gameStatus: GameStatus = GameStatus.Ongoing,
  val claimableDrawReason: DrawReason? = null,
  val boardLayout: OfflineBoardLayout = OfflineBoardLayout.STANDARD,
  val clock: ChessClock? = null,
  val isLoading: Boolean = false,
) {
  val isClockPaused: Boolean
    get() = gameStatus == GameStatus.Ongoing && clock != null && clock.activeSide != null && !clock.isRunning
}

sealed interface OfflineGameMessage {
  data class Board(val message: BoardMessage) : OfflineGameMessage

  data class PromotionSelected(val piece: PromotionPiece) : OfflineGameMessage

  data object PromotionDismissed : OfflineGameMessage

  data object ClaimDraw : OfflineGameMessage

  data class BoardLayoutSelected(val layout: OfflineBoardLayout) : OfflineGameMessage

  data class NewGame(val timeControl: TimeControl?) : OfflineGameMessage

  data object PauseClock : OfflineGameMessage

  data object ResumeClock : OfflineGameMessage

  data object ClockExpired : OfflineGameMessage

  data class GameLoaded(val snapshot: OfflineGameSnapshot?) : OfflineGameMessage
}

sealed interface OfflineGameCommand {
  data object LoadGame : OfflineGameCommand

  data class StartNewGame(val boardLayout: OfflineBoardLayout, val timeControl: TimeControl?) : OfflineGameCommand

  data class AppendMove(val halfMoveIndex: Int, val move: ChessMove, val clock: OfflineClock?) : OfflineGameCommand

  data class UpdateBoardLayout(val boardLayout: OfflineBoardLayout) : OfflineGameCommand

  data class UpdateDrawReason(val drawReason: DrawReason) : OfflineGameCommand

  data class UpdateClock(val clock: OfflineClock) : OfflineGameCommand

  data class UpdateFlaggedSide(val flaggedSide: PieceColor) : OfflineGameCommand
}

internal fun offlineGameUpdate(
  gameEngine: ChessGameEngine,
  timeSource: TimeSource.WithComparableMarks,
): Update<OfflineGameMessage, OfflineGameState, OfflineGameCommand> = update { message, currentState ->
  if (currentState.isLoading && message !is OfflineGameMessage.GameLoaded) return@update

  val now = timeSource.markNow()
  val state = flagExpiredClock(currentState, now)

  when (message) {
    is OfflineGameMessage.Board ->
      if (state.acceptsMoves()) {
        applyMove(state, state.updateBoard(message.message, gameEngine), now)
      }

    is OfflineGameMessage.PromotionSelected ->
      if (!state.isClockPaused) {
        applyMove(state, state.selectPromotion(message.piece, gameEngine), now)
      }

    OfflineGameMessage.PromotionDismissed -> state(state.copy(pendingPromotion = null))

    OfflineGameMessage.ClaimDraw -> claimDraw(state, now)

    OfflineGameMessage.PauseClock -> pauseClock(state, now)

    OfflineGameMessage.ResumeClock ->
      if (state.gameStatus == GameStatus.Ongoing && state.isClockPaused) {
        state(state.copy(clock = state.clock?.resumed(now)))
      }

    // The expired clock has already been flagged above.
    OfflineGameMessage.ClockExpired -> Unit

    is OfflineGameMessage.BoardLayoutSelected -> {
      state(state.copy(boardLayout = message.layout))
      command(OfflineGameCommand.UpdateBoardLayout(message.layout))
    }

    is OfflineGameMessage.NewGame -> {
      state(OfflineGameState(boardLayout = state.boardLayout, clock = message.timeControl?.let(::ChessClock)))
      command(OfflineGameCommand.StartNewGame(state.boardLayout, message.timeControl))
    }

    is OfflineGameMessage.GameLoaded -> {
      val snapshot = message.snapshot
      if (snapshot == null) {
        state(state.copy(isLoading = false))
        command(OfflineGameCommand.StartNewGame(state.boardLayout, timeControl = null))
      } else {
        state(state.restoredFrom(snapshot, gameEngine))
      }
    }
  }
}

private typealias OfflineGameUpdateDsl = UpdateDsl<OfflineGameState, OfflineGameCommand>

// A flag the expiry timer has not reported yet ends the game before the message is handled.
private fun OfflineGameUpdateDsl.flagExpiredClock(state: OfflineGameState, now: ComparableTimeMark): OfflineGameState {
  val flaggedSide = state.clock?.takeIf { state.gameStatus == GameStatus.Ongoing }?.expiredSide(now) ?: return state
  val flaggedState = state.flagged(flaggedSide, now)
  state(flaggedState)
  commands(flaggedState.storedClockCommands())
  command(OfflineGameCommand.UpdateFlaggedSide(flaggedSide))
  return flaggedState
}

private fun OfflineGameUpdateDsl.applyMove(
  state: OfflineGameState,
  movedState: OfflineGameState,
  now: ComparableTimeMark,
) {
  val updatedState = movedState.clockedAfterMove(state, now)
  state(updatedState)
  commands(updatedState.appendedMoveCommands(state))
}

private fun OfflineGameUpdateDsl.claimDraw(state: OfflineGameState, now: ComparableTimeMark) {
  val drawReason = state.claimableDrawReason
  if (state.gameStatus != GameStatus.Ongoing || drawReason == null) return
  val drawnState =
    state.copy(
      gameStatus = GameStatus.Draw(drawReason),
      claimableDrawReason = null,
      clock = state.clock?.paused(now),
    )
  state(drawnState)
  command(OfflineGameCommand.UpdateDrawReason(drawReason))
  commands(drawnState.storedClockCommands())
}

private fun OfflineGameUpdateDsl.pauseClock(state: OfflineGameState, now: ComparableTimeMark) {
  if (state.gameStatus != GameStatus.Ongoing || state.clock?.isRunning != true) return
  // A paused clock takes no moves, so a started move is dropped rather than left unanswerable.
  val pausedState = state.withoutMoveInProgress().copy(clock = state.clock.paused(now))
  state(pausedState)
  commands(pausedState.storedClockCommands())
}

private fun OfflineGameState.acceptsMoves(): Boolean =
  pendingPromotion == null && gameStatus == GameStatus.Ongoing && !isClockPaused

private fun OfflineGameState.hasNewMove(previousState: OfflineGameState): Boolean =
  board.position.moveHistory.size > previousState.board.position.moveHistory.size

private fun OfflineGameState.appendedMoveCommands(previousState: OfflineGameState): List<OfflineGameCommand> {
  val moveHistory = board.position.moveHistory
  return if (hasNewMove(previousState)) {
    listOf(
      OfflineGameCommand.AppendMove(
        halfMoveIndex = moveHistory.lastIndex,
        move = moveHistory.last(),
        clock = clock?.toOfflineClock(),
      ),
    )
  } else {
    emptyList()
  }
}

private fun OfflineGameState.clockedAfterMove(
  previousState: OfflineGameState,
  now: ComparableTimeMark,
): OfflineGameState {
  if (!hasNewMove(previousState)) return this
  val movedClock = clock?.afterMove(previousState.board.position.sideToMove, now)
  return copy(clock = if (gameStatus == GameStatus.Ongoing) movedClock else movedClock?.paused(now))
}

private fun OfflineGameState.flagged(side: PieceColor, now: ComparableTimeMark): OfflineGameState =
  withoutMoveInProgress().copy(
    gameStatus = board.position.timeoutStatus(flagged = side),
    claimableDrawReason = null,
    clock = clock?.paused(now),
  )

private fun OfflineGameState.withoutMoveInProgress(): OfflineGameState = copy(
  board = board.copy(selectedSquare = null, legalTargets = emptySet()),
  pendingPromotion = null,
)

private fun OfflineGameState.storedClockCommands(): List<OfflineGameCommand> =
  listOfNotNull(clock?.let { OfflineGameCommand.UpdateClock(it.toOfflineClock()) })

private fun ChessClock.toOfflineClock(): OfflineClock =
  OfflineClock(timeControl = timeControl, whiteRemaining = whiteRemaining, blackRemaining = blackRemaining)

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

    is BoardMessage.PieceDragStart -> selectSquare(message, gameEngine)

    is BoardMessage.PieceDrop ->
      if (message.to in board.legalTargets) {
        applyMove(ChessMove(from = message.from, to = message.to), gameEngine)
      } else {
        selectSquare(message, gameEngine)
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

private fun OfflineGameState.selectSquare(message: BoardMessage, gameEngine: ChessGameEngine): OfflineGameState {
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
  val status =
    snapshot.drawReason?.let(GameStatus::Draw)
      ?: snapshot.flaggedSide?.let { position.timeoutStatus(flagged = it) }
      ?: gameEngine.gameStatus(position)
  return copy(
    board = BoardState(position = position, checkedKingSquare = gameEngine.checkedKingSquare(position)),
    gameStatus = status,
    claimableDrawReason =
      if (status == GameStatus.Ongoing) gameEngine.claimableDrawReason(position) else null,
    boardLayout = snapshot.boardLayout,
    clock = snapshot.clock?.let { ChessClock.pausedOn(position, it.timeControl, it.whiteRemaining, it.blackRemaining) },
    isLoading = false,
  )
}

internal fun offlineGameCommandExecutor(
  repository: OfflineGameRepository,
): CommandExecutor<OfflineGameCommand, OfflineGameMessage> = { command ->
  when (command) {
    OfflineGameCommand.LoadGame -> flow { emit(OfflineGameMessage.GameLoaded(repository.load())) }

    is OfflineGameCommand.StartNewGame -> persist { repository.startNewGame(command.boardLayout, command.timeControl) }

    is OfflineGameCommand.AppendMove ->
      persist { repository.appendMove(command.halfMoveIndex, command.move, command.clock) }

    is OfflineGameCommand.UpdateBoardLayout -> persist { repository.updateBoardLayout(command.boardLayout) }

    is OfflineGameCommand.UpdateDrawReason -> persist { repository.updateDrawReason(command.drawReason) }

    is OfflineGameCommand.UpdateClock -> persist { repository.updateClock(command.clock) }

    is OfflineGameCommand.UpdateFlaggedSide -> persist { repository.updateFlaggedSide(command.flaggedSide) }
  }
}

// The update re-checks the clock on arrival, so the timer only has to wake it at the deadline.
@OptIn(ExperimentalCoroutinesApi::class)
private val clockExpiry: Subscription<OfflineGameState, OfflineGameMessage> = { state ->
  state
    .map { it.clock?.deadline() }
    .distinctUntilChanged()
    .flatMapLatest { deadline ->
      if (deadline == null) {
        emptyFlow()
      } else {
        flow {
          delay(-deadline.elapsedNow())
          emit(OfflineGameMessage.ClockExpired)
        }
      }
    }
}

// A write must finish even if the screen is closed right after the move that produced it.
private fun persist(write: suspend () -> Unit): Flow<OfflineGameMessage> = flow {
  withContext(NonCancellable) { write() }
}

@ViewModelScoped
class OfflineGameStore @Inject constructor(
  gameEngine: ChessGameEngine,
  repository: OfflineGameRepository,
  timeSource: TimeSource.WithComparableMarks,
) : MVUStore<OfflineGameMessage, OfflineGameState, OfflineGameCommand>(
    initialState = OfflineGameState(isLoading = true),
    update = offlineGameUpdate(gameEngine, timeSource),
    commandExecutor = offlineGameCommandExecutor(repository),
    initialCommands = listOf(OfflineGameCommand.LoadGame),
    subscriptions = listOf(clockExpiry),
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
