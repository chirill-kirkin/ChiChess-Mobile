package com.github.chirillkirkin.chichess.feature.game.online.domain

import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.Fen
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor

/** A live event received on the game channel, already mapped to domain types. */
sealed interface OnlineGameEvent {
  data class Snapshot(
    val snapshot: OnlineGameSnapshot,
    val opponentConnected: Boolean,
  ) : OnlineGameEvent

  data class PlayerJoined(val color: PieceColor) : OnlineGameEvent

  data class PlayerLeft(val color: PieceColor) : OnlineGameEvent

  data class MoveApplied(
    val revision: Long,
    val fen: Fen,
    val status: OnlineGameStatus,
    val lastMove: ChessMove?,
    val result: OnlineGameResult?,
    val terminationReason: OnlineTerminationReason?,
  ) : OnlineGameEvent

  data class GameFinished(
    val revision: Long,
    val result: OnlineGameResult,
    val terminationReason: OnlineTerminationReason,
  ) : OnlineGameEvent

  data class DrawOffered(val by: PieceColor) : OnlineGameEvent

  data object DrawDeclined : OnlineGameEvent

  data class CommandRejected(val commandId: String?, val reason: CommandRejection) : OnlineGameEvent

  data object ProtocolError : OnlineGameEvent

  /**
   * The socket closed or failed. [permanent] is true when the server refused the game (not found or
   * not a participant), so reconnecting cannot help.
   */
  data class Closed(val permanent: Boolean) : OnlineGameEvent
}
