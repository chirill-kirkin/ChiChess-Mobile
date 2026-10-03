package com.github.chirillkirkin.chichess.feature.game.online.presentation.game

import com.github.chirillkirkin.chichess.feature.game.board.BoardMessage
import com.github.chirillkirkin.chichess.feature.game.board.BoardState
import com.github.chirillkirkin.chichess.feature.game.board.boardUpdate
import com.github.chirillkirkin.chichess.feature.game.domain.ChessGameEngine
import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.ChessPosition
import com.github.chirillkirkin.chichess.feature.game.domain.MoveApplicationResult
import com.github.chirillkirkin.chichess.feature.game.domain.MoveRejectionReason
import com.github.chirillkirkin.chichess.feature.game.domain.PromotionPiece
import com.github.chirillkirkin.chichess.feature.game.domain.Square
import com.github.chirillkirkin.chichess.feature.game.online.domain.CommandRejection
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameEvent
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameStatus
import com.github.chirillkirkin.mvu.Update
import com.github.chirillkirkin.mvu.UpdateDsl
import com.github.chirillkirkin.mvu.update
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val InitialReconnectDelay = 1.seconds
private val MaxReconnectDelay = 30.seconds

internal fun onlineGameUpdate(
  gameEngine: ChessGameEngine,
  newCommandId: () -> String,
): Update<OnlineGameMessage, OnlineGameState, OnlineGameCommand> =
  update { message, state ->
    when (message) {
      is OnlineGameMessage.Event -> onEvent(message.event, state, gameEngine, newCommandId)
      is OnlineGameMessage.Board -> onBoardClick(message.message, state, gameEngine, newCommandId)
      is OnlineGameMessage.PromotionSelected -> onPromotionSelected(message.piece, state, gameEngine, newCommandId)
      OnlineGameMessage.PromotionDismissed -> state(state.copy(pendingPromotion = null))
      OnlineGameMessage.Resign ->
        if (state.canPlay()) command(OnlineGameCommand.SendResign(newCommandId(), state.revision))
      OnlineGameMessage.OfferDraw ->
        if (state.canOfferDraw()) command(OnlineGameCommand.SendOfferDraw(newCommandId()))
      OnlineGameMessage.AcceptDraw ->
        if (state.opponentOfferedDraw()) command(OnlineGameCommand.SendAcceptDraw(newCommandId()))
      OnlineGameMessage.DeclineDraw ->
        if (state.opponentOfferedDraw()) command(OnlineGameCommand.SendDeclineDraw(newCommandId()))
      OnlineGameMessage.ClaimDraw ->
        if (state.canPlay()) command(OnlineGameCommand.SendClaimDraw(newCommandId(), state.revision))
      OnlineGameMessage.DismissError -> state(state.copy(moveError = null))
      OnlineGameMessage.AppForegrounded ->
        if (state.connection != ConnectionStatus.CLOSED) {
          state(state.copy(connection = ConnectionStatus.CONNECTING, reconnectAttempt = 0))
          command(OnlineGameCommand.Connect())
        }
      OnlineGameMessage.AppBackgrounded -> command(OnlineGameCommand.Disconnect)
    }
  }

private fun UpdateDsl<OnlineGameState, OnlineGameCommand>.onEvent(
  event: OnlineGameEvent,
  state: OnlineGameState,
  engine: ChessGameEngine,
  newCommandId: () -> String,
) {
  when (event) {
    is OnlineGameEvent.Snapshot -> state(state.withSnapshot(event, engine))
    is OnlineGameEvent.MoveApplied -> state(state.withMoveApplied(event, engine))
    is OnlineGameEvent.GameFinished -> state(state.finished(event))
    is OnlineGameEvent.DrawOffered -> state(state.copy(pendingDrawOfferBy = event.by))
    OnlineGameEvent.DrawDeclined -> state(state.copy(pendingDrawOfferBy = null))
    // PLAYER_JOINED carries no status, so pull a fresh snapshot to learn the game is now in progress.
    is OnlineGameEvent.PlayerJoined -> {
      if (event.color != state.yourColor) state(state.copy(opponentConnected = true))
      command(OnlineGameCommand.RequestSync(newCommandId()))
    }
    is OnlineGameEvent.PlayerLeft ->
      if (event.color != state.yourColor) state(state.copy(opponentConnected = false))
    is OnlineGameEvent.CommandRejected -> state(state.rolledBack(event.reason, engine))
    is OnlineGameEvent.Closed -> onClosed(event, state, engine)
  }
}

private fun UpdateDsl<OnlineGameState, OnlineGameCommand>.onClosed(
  event: OnlineGameEvent.Closed,
  state: OnlineGameState,
  engine: ChessGameEngine,
) {
  val discarded = state.withoutPendingMove(engine)
  if (event.permanent || state.result != null) {
    state(discarded.copy(connection = ConnectionStatus.CLOSED))
  } else {
    val attempt = state.reconnectAttempt + 1
    state(discarded.copy(connection = ConnectionStatus.CONNECTING, reconnectAttempt = attempt))
    command(OnlineGameCommand.Connect(reconnectDelay(attempt)))
  }
}

private fun reconnectDelay(attempt: Int): Duration {
  var delay = InitialReconnectDelay
  repeat(attempt - 1) { delay = minOf(delay * 2, MaxReconnectDelay) }
  return delay
}

private fun UpdateDsl<OnlineGameState, OnlineGameCommand>.onBoardClick(
  message: BoardMessage,
  state: OnlineGameState,
  engine: ChessGameEngine,
  newCommandId: () -> String,
) {
  val board = state.board
  if (board == null || !state.canPlay() || !state.isYourTurn() || state.pendingMove != null || state.pendingPromotion != null) {
    return
  }
  when (message) {
    is BoardMessage.SquareClick -> {
      val selected = board.selectedSquare
      if (
        selected == null ||
        selected == message.square ||
        board.position[selected]?.color == board.position[message.square]?.color
      ) {
        state(state.copy(board = board.select(message, engine)))
      } else {
        applyOptimisticMove(state, ChessMove(from = selected, to = message.square), engine, newCommandId)
      }
    }
  }
}

private fun UpdateDsl<OnlineGameState, OnlineGameCommand>.onPromotionSelected(
  piece: PromotionPiece,
  state: OnlineGameState,
  engine: ChessGameEngine,
  newCommandId: () -> String,
) {
  val move = state.pendingPromotion ?: return
  applyOptimisticMove(state.copy(pendingPromotion = null), move.copy(promotion = piece), engine, newCommandId)
}

private fun UpdateDsl<OnlineGameState, OnlineGameCommand>.applyOptimisticMove(
  state: OnlineGameState,
  move: ChessMove,
  engine: ChessGameEngine,
  newCommandId: () -> String,
) {
  val board = state.board ?: return
  when (val result = engine.applyMove(board.position, move)) {
    is MoveApplicationResult.Applied -> {
      val commandId = newCommandId()
      state(
        state.copy(
          board = engine.boardOf(result.position),
          pendingMove = PendingMove(commandId, state.revision, move),
          pendingPromotion = null,
          moveError = null,
        ),
      )
      command(OnlineGameCommand.SendMove(commandId, state.revision, move))
    }

    is MoveApplicationResult.Rejected ->
      if (result.reason == MoveRejectionReason.PROMOTION_REQUIRED && move.promotion == null) {
        state(state.copy(pendingPromotion = move))
      }
  }
}

private fun OnlineGameState.withSnapshot(
  event: OnlineGameEvent.Snapshot,
  engine: ChessGameEngine,
): OnlineGameState {
  val snapshot = event.snapshot
  val position = engine.positionFromFen(snapshot.fen)
  return copy(
    connection = ConnectionStatus.CONNECTED,
    inviteCode = snapshot.inviteCode,
    yourColor = snapshot.yourColor,
    confirmedPosition = position,
    board = engine.boardOf(position),
    lastMove = snapshot.lastMove,
    revision = snapshot.revision,
    status = snapshot.status,
    opponentConnected = event.opponentConnected,
    result = snapshot.result,
    terminationReason = snapshot.terminationReason,
    pendingDrawOfferBy = snapshot.pendingDrawOfferBy,
    pendingMove = null,
    pendingPromotion = null,
    moveError = null,
    reconnectAttempt = 0,
  )
}

private fun OnlineGameState.withMoveApplied(
  event: OnlineGameEvent.MoveApplied,
  engine: ChessGameEngine,
): OnlineGameState {
  val position = engine.positionFromFen(event.fen)
  return copy(
    confirmedPosition = position,
    board = engine.boardOf(position),
    lastMove = event.lastMove,
    revision = event.revision,
    status = event.status,
    result = event.result,
    terminationReason = event.terminationReason,
    pendingDrawOfferBy = null,
    pendingMove = null,
    pendingPromotion = null,
    moveError = null,
  )
}

private fun OnlineGameState.finished(event: OnlineGameEvent.GameFinished): OnlineGameState =
  copy(
    status = OnlineGameStatus.FINISHED,
    result = event.result,
    terminationReason = event.terminationReason,
    revision = event.revision,
    pendingMove = null,
    pendingDrawOfferBy = null,
  )

private fun OnlineGameState.rolledBack(
  reason: CommandRejection,
  engine: ChessGameEngine,
): OnlineGameState =
  withoutPendingMove(engine).copy(
    // A revision conflict is followed by a fresh snapshot, so it re-syncs instead of surfacing.
    moveError = reason.takeUnless { it == CommandRejection.REVISION_CONFLICT },
  )

private fun OnlineGameState.withoutPendingMove(engine: ChessGameEngine): OnlineGameState =
  copy(
    board = confirmedPosition?.let(engine::boardOf) ?: board,
    pendingMove = null,
    pendingPromotion = null,
  )

private fun BoardState.select(
  message: BoardMessage.SquareClick,
  engine: ChessGameEngine,
): BoardState {
  val updated = boardUpdate(message, this)
  val legalTargets: Set<Square> =
    updated.selectedSquare?.let { selected ->
      engine.legalMoves(position).asSequence().filter { it.from == selected }.map { it.to }.toSet()
    } ?: emptySet()
  return updated.copy(legalTargets = legalTargets)
}

private fun ChessGameEngine.boardOf(position: ChessPosition): BoardState =
  BoardState(position = position, checkedKingSquare = checkedKingSquare(position))

private fun OnlineGameState.canPlay(): Boolean =
  connection == ConnectionStatus.CONNECTED && status == OnlineGameStatus.IN_PROGRESS && result == null

private fun OnlineGameState.isYourTurn(): Boolean =
  yourColor != null && board?.position?.sideToMove == yourColor

private fun OnlineGameState.opponentOfferedDraw(): Boolean =
  pendingDrawOfferBy != null && pendingDrawOfferBy != yourColor

// A draw cannot be offered before the first move has been played.
private fun OnlineGameState.canOfferDraw(): Boolean =
  canPlay() && pendingDrawOfferBy == null && lastMove != null
