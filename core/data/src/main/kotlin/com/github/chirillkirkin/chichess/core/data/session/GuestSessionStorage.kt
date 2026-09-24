package com.github.chirillkirkin.chichess.core.data.session

import com.github.chirillkirkin.chichess.core.domain.session.GuestSession

interface GuestSessionStorage {
  suspend fun read(): GuestSession?

  suspend fun save(session: GuestSession)
}
