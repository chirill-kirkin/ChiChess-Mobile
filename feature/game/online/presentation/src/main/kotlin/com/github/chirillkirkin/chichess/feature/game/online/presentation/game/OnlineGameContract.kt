package com.github.chirillkirkin.chichess.feature.game.online.presentation.game

import com.github.chirillkirkin.chichess.feature.game.board.BoardMessage
import com.github.chirillkirkin.chichess.feature.game.board.BoardState
import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.ChessPosition
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.domain.PromotionPiece
import com.github.chirillkirkin.chichess.feature.game.online.domain.CommandRejection
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameEvent
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameResult
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameStatus
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineTerminationReason
import kotlin.time.Duration

enum class ConnectionStatus { CONNECTING, CONNECTED, CLOSED }

/** A move applied locally and awaiting the server's confirmation. */
data class PendingMove(val commandId: String, val expectedRevision: Long, val move: ChessMove)

data class OnlineGameState(
  val gameId: String,
  val connection: ConnectionStatus = ConnectionStatus.CONNECTING,
  val inviteCode: String? = null,
  val yourColor: PieceColor? = null,
  // The last server-confirmed position; the board may show an optimistic move ahead of it.
  val confirmedPosition: ChessPosition? = null,
  val board: BoardState? = null,
  // The last confirmed move, or null before the first move has been played.
  val lastMove: ChessMove? = null,
  val revision: Long = 0L,
  val status: OnlineGameStatus = OnlineGameStatus.WAITING_FOR_OPPONENT,
  val opponentConnected: Boolean = true,
  val result: OnlineGameResult? = null,
  val terminationReason: OnlineTerminationReason? = null,
  val pendingDrawOfferBy: PieceColor? = null,
  val pendingMove: PendingMove? = null,
  val pendingPromotion: ChessMove? = null,
  val moveError: CommandRejection? = null,
  val reconnectAttempt: Int = 0,
)

sealed interface OnlineGameMessage {
  data class Event(val event: OnlineGameEvent) : OnlineGameMessage

  data class Board(val message: BoardMessage) : OnlineGameMessage

  data class PromotionSelected(val piece: PromotionPiece) : OnlineGameMessage

  data object PromotionDismissed : OnlineGameMessage

  data object Resign : OnlineGameMessage

  data object OfferDraw : OnlineGameMessage

  data object AcceptDraw : OnlineGameMessage

  data object DeclineDraw : OnlineGameMessage

  data object ClaimDraw : OnlineGameMessage

  data object DismissError : OnlineGameMessage

  data object AppForegrounded : OnlineGameMessage

  data object AppBackgrounded : OnlineGameMessage
}

sealed interface OnlineGameCommand {
  data class Connect(val delay: Duration = Duration.ZERO) : OnlineGameCommand

  data object Disconnect : OnlineGameCommand

  data class RequestSync(val commandId: String) : OnlineGameCommand

  data class SendMove(val commandId: String, val expectedRevision: Long, val move: ChessMove) : OnlineGameCommand

  data class SendResign(val commandId: String, val expectedRevision: Long) : OnlineGameCommand

  data class SendOfferDraw(val commandId: String) : OnlineGameCommand

  data class SendAcceptDraw(val commandId: String) : OnlineGameCommand

  data class SendDeclineDraw(val commandId: String) : OnlineGameCommand

  data class SendClaimDraw(val commandId: String, val expectedRevision: Long) : OnlineGameCommand

  data object ExitOnError : OnlineGameCommand
}
