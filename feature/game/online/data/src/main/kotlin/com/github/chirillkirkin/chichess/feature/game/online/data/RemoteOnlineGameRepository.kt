package com.github.chirillkirkin.chichess.feature.game.online.data

import com.github.chirillkirkin.chichess.feature.game.online.domain.CreatedGame
import com.github.chirillkirkin.chichess.feature.game.online.domain.JoinGameResult
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameRepository
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameSnapshot
import com.github.chirillkirkin.chichess.feature.game.online.domain.SnapshotResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerializationException

internal const val GAME_PATH = "/game"
internal const val GAME_JOIN_PATH = "/game/join"
internal const val GAMES_HISTORY_PATH = "/games/history"

internal const val GAME_NOT_FOUND_CODE = "GAME_NOT_FOUND"
internal const val CANNOT_JOIN_OWN_GAME_CODE = "CANNOT_JOIN_OWN_GAME"
internal const val GAME_ALREADY_JOINED_CODE = "GAME_ALREADY_JOINED"
internal const val NOT_A_GAME_PARTICIPANT_CODE = "NOT_A_GAME_PARTICIPANT"

class RemoteOnlineGameRepository(private val httpClient: HttpClient) : OnlineGameRepository {
  override suspend fun createGame(): CreatedGame = httpClient.post(GAME_PATH).body<CreateGameResponse>().toCreatedGame()

  override suspend fun joinGame(inviteCode: String): JoinGameResult {
    val response = httpClient.post(GAME_JOIN_PATH) {
      contentType(ContentType.Application.Json)
      setBody(JoinGameRequest(inviteCode))
    }
    if (response.status.isSuccess()) {
      return JoinGameResult.Joined(response.body<JoinGameResponse>().gameId)
    }
    return when (response.errorCode()) {
      GAME_NOT_FOUND_CODE -> JoinGameResult.NotFound
      CANNOT_JOIN_OWN_GAME_CODE -> JoinGameResult.OwnGame
      GAME_ALREADY_JOINED_CODE -> JoinGameResult.AlreadyJoined
      else -> throw UnexpectedResponse(response.status.value, response.errorCode())
    }
  }

  override suspend fun snapshot(gameId: String): SnapshotResult {
    val response = httpClient.get("$GAME_PATH/$gameId")
    if (response.status.isSuccess()) {
      return SnapshotResult.Success(response.body<GameSnapshotResponse>().toSnapshot())
    }
    return when (response.errorCode()) {
      GAME_NOT_FOUND_CODE -> SnapshotResult.NotFound
      NOT_A_GAME_PARTICIPANT_CODE -> SnapshotResult.NotParticipant
      else -> throw UnexpectedResponse(response.status.value, response.errorCode())
    }
  }

  override suspend fun history(): List<OnlineGameSnapshot> = httpClient
    .get(GAMES_HISTORY_PATH)
    .body<List<GameSnapshotResponse>>()
    .map(GameSnapshotResponse::toSnapshot)

  private suspend fun HttpResponse.errorCode(): String? = try {
    body<ApiErrorResponse>().code
  } catch (_: SerializationException) {
    null
  }
}

class UnexpectedResponse(val status: Int, val code: String?) :
  RuntimeException("Unexpected response $status" + code?.let { " (code=$it)" }.orEmpty())
