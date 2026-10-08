package com.github.chirillkirkin.chichess.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val STALE_TOKEN = "stale-token"
private const val FRESH_TOKEN = "fresh-token"

class BearerAuthTest {
  @Test
  fun `refreshes the token and retries the request after 401`() = runTest {
    val sentTokens = mutableListOf<String?>()
    val tokenProvider = FakeBearerTokenProvider()
    val client = authenticatedClient(tokenProvider) { token ->
      sentTokens += token
      if (token == "Bearer $FRESH_TOKEN") HttpStatusCode.OK else HttpStatusCode.Unauthorized
    }

    val response = client.get("/game")

    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals(listOf<String?>("Bearer $STALE_TOKEN", "Bearer $FRESH_TOKEN"), sentTokens)
    assertEquals(1, tokenProvider.refreshCount)
  }

  @Test
  fun `does not refresh again when the refreshed token is also rejected`() = runTest {
    val tokenProvider = FakeBearerTokenProvider()
    val client = authenticatedClient(tokenProvider) { HttpStatusCode.Unauthorized }

    val response = client.get("/game")

    assertEquals(HttpStatusCode.Unauthorized, response.status)
    assertEquals(1, tokenProvider.refreshCount)
  }

  private fun authenticatedClient(
    tokenProvider: BearerTokenProvider,
    statusFor: (authorization: String?) -> HttpStatusCode,
  ): HttpClient {
    val engine = MockEngine { request ->
      respond(content = "", status = statusFor(request.headers[HttpHeaders.Authorization]))
    }
    return HttpClient(engine) {
      configureChiChessClient("http://localhost")
      installBearerAuth(tokenProvider)
    }
  }
}

private class FakeBearerTokenProvider : BearerTokenProvider {
  var refreshCount = 0
    private set

  override suspend fun currentToken(): String = STALE_TOKEN

  override suspend fun refreshToken(): String {
    refreshCount++
    return FRESH_TOKEN
  }
}
