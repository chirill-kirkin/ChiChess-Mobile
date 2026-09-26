package com.github.chirillkirkin.chichess.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.HttpHeaders
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import timber.log.Timber

val chiChessJson: Json = Json {
  ignoreUnknownKeys = true
}

fun interface BearerTokenProvider {
  suspend fun currentToken(): String?
}

private val timberKtorLogger = object : Logger {
  override fun log(message: String) {
    Timber.tag("Ktor").v(message)
  }
}

fun HttpClientConfig<*>.configureChiChessClient(
  baseUrl: String,
  verboseLogging: Boolean = false,
) {
  install(ContentNegotiation) {
    json(chiChessJson)
  }
  install(Logging) {
    logger = timberKtorLogger
    level = if (verboseLogging) LogLevel.ALL else LogLevel.NONE
    sanitizeHeader { header -> header.equals(HttpHeaders.Authorization, ignoreCase = true) }
  }
  defaultRequest {
    url.takeFrom(baseUrl)
  }
}

fun HttpClientConfig<*>.installBearerAuth(tokenProvider: BearerTokenProvider) {
  install(Auth) {
    bearer {
      loadTokens { tokenProvider.currentToken()?.let { BearerTokens(it, "") } }
      sendWithoutRequest { true }
    }
  }
}

/** Unauthenticated client for the guest-session bootstrap (`POST /sessions/guest`). */
fun createChiChessHttpClient(
  baseUrl: String,
  verboseLogging: Boolean = false,
): HttpClient =
  HttpClient(OkHttp) {
    configureChiChessClient(baseUrl, verboseLogging)
  }

/**
 * Authenticated client for game HTTP endpoints and the live game WebSocket; attaches a bearer token
 * from [tokenProvider], including on the WebSocket handshake.
 */
fun createAuthenticatedChiChessHttpClient(
  baseUrl: String,
  verboseLogging: Boolean = false,
  tokenProvider: BearerTokenProvider,
): HttpClient =
  HttpClient(OkHttp) {
    configureChiChessClient(baseUrl, verboseLogging)
    installBearerAuth(tokenProvider)
    install(WebSockets)
  }
