package com.github.chirillkirkin.chichess.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val chiChessJson: Json = Json {
  ignoreUnknownKeys = true
}

fun interface BearerTokenProvider {
  suspend fun currentToken(): String?
}

fun HttpClientConfig<*>.configureChiChessClient(baseUrl: String) {
  install(ContentNegotiation) {
    json(chiChessJson)
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
fun createChiChessHttpClient(baseUrl: String): HttpClient =
  HttpClient(OkHttp) {
    configureChiChessClient(baseUrl)
  }

/** Authenticated client for game endpoints; attaches a bearer token from [tokenProvider]. */
fun createAuthenticatedChiChessHttpClient(
  baseUrl: String,
  tokenProvider: BearerTokenProvider,
): HttpClient =
  HttpClient(OkHttp) {
    configureChiChessClient(baseUrl)
    installBearerAuth(tokenProvider)
  }
