package com.github.chirillkirkin.chichess.core.domain.session

interface GuestSessionRepository {
  suspend fun currentSession(): GuestSession

  suspend fun refreshSession(): GuestSession
}
