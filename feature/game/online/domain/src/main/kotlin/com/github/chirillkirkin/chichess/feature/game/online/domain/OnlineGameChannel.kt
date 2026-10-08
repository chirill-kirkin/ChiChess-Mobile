package com.github.chirillkirkin.chichess.feature.game.online.domain

import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import kotlinx.coroutines.flow.Flow

interface OnlineGameChannel {
  suspend fun connect(gameId: String): OnlineGameSession
}

class OnlineGameConnectionException(cause: Throwable) : Exception(cause)

/**
 * A connected game socket. The caller owns each [commandId] so a command can be safely re-sent after
 * a reconnect; the server records it and stays idempotent.
 */
interface OnlineGameSession {
  val events: Flow<OnlineGameEvent>

  suspend fun requestSync(commandId: String)

  suspend fun makeMove(commandId: String, expectedRevision: Long, move: ChessMove)

  suspend fun resign(commandId: String, expectedRevision: Long)

  suspend fun offerDraw(commandId: String)

  suspend fun acceptDraw(commandId: String)

  suspend fun declineDraw(commandId: String)

  suspend fun claimDraw(commandId: String, expectedRevision: Long)

  suspend fun close()
}
