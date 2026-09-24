package com.github.chirillkirkin.chichess.core.data.session

import com.github.chirillkirkin.chichess.core.domain.session.GuestSession
import com.github.chirillkirkin.chichess.core.domain.session.GuestSessionRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal const val GUEST_SESSION_PATH = "/sessions/guest"

class RemoteGuestSessionRepository(
  private val httpClient: HttpClient,
  private val sessionStorage: GuestSessionStorage,
) : GuestSessionRepository {
  // Serialize bootstrapping so concurrent callers create at most one session.
  private val mutex = Mutex()
  private var cached: GuestSession? = null

  override suspend fun currentSession(): GuestSession {
    cached?.let { return it }
    return mutex.withLock {
      cached?.let { return it }
      val session = sessionStorage.read() ?: createSession()
      session.also { cached = it }
    }
  }

  private suspend fun createSession(): GuestSession {
    val session = httpClient.post(GUEST_SESSION_PATH).body<GuestSessionResponse>().toGuestSession()
    sessionStorage.save(session)
    return session
  }
}
