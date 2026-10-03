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
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json
import timber.log.Timber

val chiChessJson: Json = Json {
  ignoreUnknownKeys = true
}

interface BearerTokenProvider {
  suspend fun currentToken(): String?

  suspend fun refreshToken(): String?
}

private val WebSocketPingInterval = 15.seconds

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
      refreshTokens { tokenProvider.refreshToken()?.let { BearerTokens(it, "") } }
      sendWithoutRequest { true }
    }
  }
}

fun createChiChessHttpClient(
  baseUrl: String,
  verboseLogging: Boolean = false,
): HttpClient =
  HttpClient(OkHttp) {
    configureChiChessClient(baseUrl, verboseLogging)
  }

fun createAuthenticatedChiChessHttpClient(
  baseUrl: String,
  verboseLogging: Boolean = false,
  tokenProvider: BearerTokenProvider,
): HttpClient =
  HttpClient(OkHttp) {
    engine {
      config { pingInterval(WebSocketPingInterval.inWholeMilliseconds, TimeUnit.MILLISECONDS) }
    }
    configureChiChessClient(baseUrl, verboseLogging)
    installBearerAuth(tokenProvider)
    install(WebSockets)
  }
