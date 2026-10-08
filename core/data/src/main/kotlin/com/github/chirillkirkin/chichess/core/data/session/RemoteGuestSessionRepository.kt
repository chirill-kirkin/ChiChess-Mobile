package com.github.chirillkirkin.chichess.core.data.session

import com.github.chirillkirkin.chichess.core.domain.session.GuestSession
import com.github.chirillkirkin.chichess.core.domain.session.GuestSessionRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post

internal const val GUEST_SESSION_PATH = "/sessions/guest"

class RemoteGuestSessionRepository(
  private val httpClient: HttpClient,
  private val sessionStorage: GuestSessionStorage,
) : GuestSessionRepository {
  override suspend fun currentSession(): GuestSession = sessionStorage.read() ?: createSession()

  override suspend fun refreshSession(): GuestSession = createSession()

  private suspend fun createSession(): GuestSession {
    val session = httpClient.post(GUEST_SESSION_PATH).body<GuestSessionResponse>().toGuestSession()
    sessionStorage.save(session)
    return session
  }
}
