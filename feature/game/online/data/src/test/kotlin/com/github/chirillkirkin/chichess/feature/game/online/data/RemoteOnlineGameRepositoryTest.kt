package com.github.chirillkirkin.chichess.feature.game.online.data

import com.github.chirillkirkin.chichess.core.data.network.BearerTokenProvider
import com.github.chirillkirkin.chichess.core.data.network.configureChiChessClient
import com.github.chirillkirkin.chichess.core.data.network.installBearerAuth
import com.github.chirillkirkin.chichess.feature.game.domain.Fen
import com.github.chirillkirkin.chichess.feature.game.domain.PieceColor
import com.github.chirillkirkin.chichess.feature.game.online.domain.CreatedGame
import com.github.chirillkirkin.chichess.feature.game.online.domain.JoinGameResult
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameResult
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineGameStatus
import com.github.chirillkirkin.chichess.feature.game.online.domain.OnlineTerminationReason
import com.github.chirillkirkin.chichess.feature.game.online.domain.SnapshotResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

private const val TOKEN = "test-token"
private const val GAME_ID = "game-1"
private const val INVITE_CODE = "CODE123456"
private const val REVISION = 5L
private const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

class RemoteOnlineGameRepositoryTest {
  @Test
  fun `createGame sends bearer token and maps the response`() = runTest {
    val requests = mutableListOf<RecordedRequest>()
    val repository = repository(requests, HttpStatusCode.Created, """{"gameId":"$GAME_ID","inviteCode":"$INVITE_CODE"}""")

    val created = repository.createGame()

    assertEquals(CreatedGame(gameId = GAME_ID, inviteCode = INVITE_CODE), created)
    val request = requests.single()
    assertEquals(HttpMethod.Post, request.method)
    assertEquals(GAME_PATH, request.path)
    assertEquals("Bearer $TOKEN", request.authorization)
  }

  @Test
  fun `joinGame sends the invite code and returns Joined`() = runTest {
    val requests = mutableListOf<RecordedRequest>()
    val repository = repository(requests, HttpStatusCode.OK, """{"gameId":"$GAME_ID"}""")

    val result = repository.joinGame(INVITE_CODE)

    assertEquals(JoinGameResult.Joined(GAME_ID), result)
    val request = requests.single()
    assertEquals(GAME_JOIN_PATH, request.path)
    assertEquals(request.body?.contains(INVITE_CODE), true)
  }

  @Test
  fun `joinGame maps GAME_NOT_FOUND`() = runTest {
    val result = repository(status = HttpStatusCode.NotFound, body = errorBody(GAME_NOT_FOUND_CODE)).joinGame(INVITE_CODE)
    assertEquals(JoinGameResult.NotFound, result)
  }

  @Test
  fun `joinGame maps CANNOT_JOIN_OWN_GAME`() = runTest {
    val result = repository(status = HttpStatusCode.Conflict, body = errorBody(CANNOT_JOIN_OWN_GAME_CODE)).joinGame(INVITE_CODE)
    assertEquals(JoinGameResult.OwnGame, result)
  }

  @Test
  fun `joinGame maps GAME_ALREADY_JOINED`() = runTest {
    val result = repository(status = HttpStatusCode.Conflict, body = errorBody(GAME_ALREADY_JOINED_CODE)).joinGame(INVITE_CODE)
    assertEquals(JoinGameResult.AlreadyJoined, result)
  }

  @Test
  fun `snapshot maps the response for a participant`() = runTest {
    val requests = mutableListOf<RecordedRequest>()
    val repository = repository(requests, HttpStatusCode.OK, snapshotBody(color = "BLACK", status = "IN_PROGRESS"))

    val result = repository.snapshot(GAME_ID)

    val snapshot = (result as SnapshotResult.Success).snapshot
    assertEquals(PieceColor.BLACK, snapshot.yourColor)
    assertEquals(OnlineGameStatus.IN_PROGRESS, snapshot.status)
    assertEquals(REVISION, snapshot.revision)
    assertEquals(Fen(START_FEN), snapshot.fen)
    assertNull(snapshot.result)
    assertEquals("$GAME_PATH/$GAME_ID", requests.single().path)
  }

  @Test
  fun `snapshot maps NOT_A_GAME_PARTICIPANT`() = runTest {
    val result = repository(status = HttpStatusCode.Forbidden, body = errorBody(NOT_A_GAME_PARTICIPANT_CODE)).snapshot(GAME_ID)
    assertEquals(SnapshotResult.NotParticipant, result)
  }

  @Test
  fun `snapshot maps GAME_NOT_FOUND`() = runTest {
    val result = repository(status = HttpStatusCode.NotFound, body = errorBody(GAME_NOT_FOUND_CODE)).snapshot(GAME_ID)
    assertEquals(SnapshotResult.NotFound, result)
  }

  @Test
  fun `history maps every snapshot`() = runTest {
    val body = "[${snapshotBody(color = "WHITE", status = "FINISHED", result = "WHITE_WON", terminationReason = "CHECKMATE")}]"
    val history = repository(status = HttpStatusCode.OK, body = body).history()

    val snapshot = history.single()
    assertEquals(PieceColor.WHITE, snapshot.yourColor)
    assertEquals(OnlineGameStatus.FINISHED, snapshot.status)
    assertEquals(OnlineGameResult.WHITE_WON, snapshot.result)
    assertEquals(OnlineTerminationReason.CHECKMATE, snapshot.terminationReason)
  }

  private fun repository(
    requests: MutableList<RecordedRequest> = mutableListOf(),
    status: HttpStatusCode,
    body: String,
  ): RemoteOnlineGameRepository {
    val engine = MockEngine { request ->
      requests += RecordedRequest(
        method = request.method,
        path = request.url.encodedPath,
        authorization = request.headers[HttpHeaders.Authorization],
        body = (request.body as? TextContent)?.text,
      )
      respond(content = body, status = status, headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }
    val client = HttpClient(engine) {
      configureChiChessClient("http://localhost")
      installBearerAuth(
        object : BearerTokenProvider {
          override suspend fun currentToken(): String = TOKEN

          override suspend fun refreshToken(): String = TOKEN
        },
      )
    }
    return RemoteOnlineGameRepository(client)
  }
}

private fun errorBody(code: String): String = """{"code":"$code"}"""

private fun snapshotBody(
  color: String,
  status: String,
  result: String? = null,
  terminationReason: String? = null,
): String =
  """{"gameId":"$GAME_ID","inviteCode":"$INVITE_CODE","yourColor":"$color","status":"$status",""" +
    """"revision":$REVISION,"fen":"$START_FEN","result":${result.asJson()},"terminationReason":${terminationReason.asJson()}}"""

private fun String?.asJson(): String = this?.let { "\"$it\"" } ?: "null"

private data class RecordedRequest(
  val method: HttpMethod,
  val path: String,
  val authorization: String?,
  val body: String?,
)
