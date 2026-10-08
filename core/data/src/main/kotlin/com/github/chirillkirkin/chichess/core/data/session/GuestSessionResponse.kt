package com.github.chirillkirkin.chichess.core.data.session

import com.github.chirillkirkin.chichess.core.domain.session.GuestSession
import kotlinx.serialization.Serializable

@Serializable
data class GuestSessionResponse(val sessionId: String, val token: String)

fun GuestSessionResponse.toGuestSession(): GuestSession = GuestSession(sessionId = sessionId, token = token)
