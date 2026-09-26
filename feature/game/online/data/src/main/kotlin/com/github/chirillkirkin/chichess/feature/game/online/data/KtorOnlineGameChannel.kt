package com.github.chirillkirkin.chichess.feature.game.online.data

import com.github.chirillkirkin.chichess.feature.game.domain.ChessMove
import com.github.chirillkirkin.chichess.feature.game.domain.toUci
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameChannel
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameEvent
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameSession
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.http.takeFrom
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

private const val GAME_SOCKET_SEGMENT = "game"
private const val GAME_SOCKET_LOG_TAG = "GameSocket"

class KtorOnlineGameChannel(
  private val httpClient: HttpClient,
  private val baseUrl: String,
) : OnlineGameChannel {
  // The client's bearer auth attaches the token to the handshake; no token in the URL.
  override suspend fun connect(gameId: String): OnlineGameSession {
    val session = httpClient.webSocketSession {
      url {
        takeFrom(baseUrl)
        protocol = if (protocol == URLProtocol.HTTPS) URLProtocol.WSS else URLProtocol.WS
        path(GAME_SOCKET_SEGMENT, gameId)
      }
    }
    return KtorOnlineGameSession(session)
  }
}

private class KtorOnlineGameSession(
  private val session: DefaultClientWebSocketSession,
) : OnlineGameSession {
  override val events: Flow<OnlineGameEvent> = flow {
    for (frame in session.incoming) {
      if (frame is Frame.Text) {
        val text = frame.readText()
        Timber.tag(GAME_SOCKET_LOG_TAG).v("← %s", text)
        emit(gameProtocolJson.decodeFromString(GameEvent.serializer(), text).toOnlineEvent())
      }
    }
    val reason = session.closeReason.await()
    Timber.tag(GAME_SOCKET_LOG_TAG).v("socket closed: code=%s reason=%s", reason?.code, reason?.message)
    emit(OnlineGameEvent.Closed(reason?.code, reason?.message))
  }

  override suspend fun requestSync(commandId: String) =
    send(RequestSync(GAME_PROTOCOL_VERSION, commandId))

  override suspend fun makeMove(commandId: String, expectedRevision: Long, move: ChessMove) =
    send(MakeMove(GAME_PROTOCOL_VERSION, commandId, expectedRevision, move.toUci()))

  override suspend fun resign(commandId: String, expectedRevision: Long) =
    send(Resign(GAME_PROTOCOL_VERSION, commandId, expectedRevision))

  override suspend fun offerDraw(commandId: String) =
    send(OfferDraw(GAME_PROTOCOL_VERSION, commandId))

  override suspend fun acceptDraw(commandId: String) =
    send(AcceptDraw(GAME_PROTOCOL_VERSION, commandId))

  override suspend fun declineDraw(commandId: String) =
    send(DeclineDraw(GAME_PROTOCOL_VERSION, commandId))

  override suspend fun claimDraw(commandId: String, expectedRevision: Long) =
    send(ClaimDraw(GAME_PROTOCOL_VERSION, commandId, expectedRevision))

  override suspend fun close() = session.close()

  private suspend fun send(command: GameCommand) {
    val text = gameProtocolJson.encodeToString(GameCommand.serializer(), command)
    Timber.tag(GAME_SOCKET_LOG_TAG).v("→ %s", text)
    session.send(Frame.Text(text))
  }
}
