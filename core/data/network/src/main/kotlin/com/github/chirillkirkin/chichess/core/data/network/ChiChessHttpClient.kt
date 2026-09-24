package com.github.chirillkirkin.chichess.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val chiChessJson: Json = Json {
  ignoreUnknownKeys = true
}

fun HttpClientConfig<*>.configureChiChessClient(baseUrl: String) {
  install(ContentNegotiation) {
    json(chiChessJson)
  }
  defaultRequest {
    url.takeFrom(baseUrl)
  }
}

fun createChiChessHttpClient(baseUrl: String): HttpClient =
  HttpClient(OkHttp) {
    configureChiChessClient(baseUrl)
  }
