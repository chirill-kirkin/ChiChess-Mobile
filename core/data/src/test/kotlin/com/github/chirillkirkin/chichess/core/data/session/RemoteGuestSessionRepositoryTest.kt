package com.github.chirillkirkin.chichess.core.data.session

import com.github.chirillkirkin.chichess.core.data.network.configureChiChessClient
import com.github.chirillkirkin.chichess.core.domain.session.GuestSession
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class RemoteGuestSessionRepositoryTest {
  @Test
  fun `bootstraps and persists a session when none is stored`() = runTest {
    val storage = FakeGuestSessionStorage()
    val requests = mutableListOf<RecordedRequest>()
    val repository = RemoteGuestSessionRepository(guestSessionClient(requests), storage)

    val session = repository.currentSession()

    assertEquals(GuestSession(sessionId = "session-1", token = "token-1"), session)
    assertEquals(session, storage.read())
    assertEquals(listOf(RecordedRequest(HttpMethod.Post, GUEST_SESSION_PATH)), requests)
  }

  @Test
  fun `reuses the created session without a second request`() = runTest {
    val requests = mutableListOf<RecordedRequest>()
    val repository = RemoteGuestSessionRepository(guestSessionClient(requests), FakeGuestSessionStorage())

    val first = repository.currentSession()
    val second = repository.currentSession()

    assertEquals(first, second)
    assertEquals(1, requests.size)
  }

  @Test
  fun `returns the stored session without any request`() = runTest {
    val stored = GuestSession(sessionId = "stored", token = "stored-token")
    val requests = mutableListOf<RecordedRequest>()
    val repository = RemoteGuestSessionRepository(guestSessionClient(requests), FakeGuestSessionStorage(stored))

    val session = repository.currentSession()

    assertEquals(stored, session)
    assertEquals(emptyList(), requests)
  }

  private fun guestSessionClient(requests: MutableList<RecordedRequest>): HttpClient {
    val engine = MockEngine { request ->
      requests += RecordedRequest(request.method, request.url.encodedPath)
      respond(
        content = """{"sessionId":"session-1","token":"token-1"}""",
        status = HttpStatusCode.Created,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    }
    return HttpClient(engine) {
      configureChiChessClient("http://localhost")
    }
  }
}

private data class RecordedRequest(
  val method: HttpMethod,
  val path: String,
)

private class FakeGuestSessionStorage(
  private var stored: GuestSession? = null,
) : GuestSessionStorage {
  override suspend fun read(): GuestSession? = stored

  override suspend fun save(session: GuestSession) {
    stored = session
  }
}
