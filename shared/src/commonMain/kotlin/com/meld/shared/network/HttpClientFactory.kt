package com.meld.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

/**
 * Motor HTTP por plataforma (Fase 02C, B3 OkHttp → Darwin).
 *
 * - iOS (`iosMain`): `Darwin` (NSURLSession, nativo, sin dependencias JVM).
 * - JVM (`jvmMain`): `CIO`, solo como host de pruebas (`jvmTest`); no es
 *   plataforma destino.
 */
expect fun platformHttpEngine(): HttpClientEngineFactory<*>

/**
 * Cliente YouTube Music iOS (adaptado de `InnerTube.createClient()`).
 *
 * Conserva del original: `expectSuccess = false` (InnerTubeX valida estados
 * y reintenta transitorios), `ContentNegotiation` con el mismo `Json`
 * (`ignoreUnknownKeys`, `explicitNulls = false`), timeouts
 * (request 60 s / connect 30 s / socket 60 s) y `defaultRequest` con la base
 * `https://music.youtube.com/youtubei/v1/` + `Accept: application/json`.
 *
 * Diferencias deliberadas (sin autenticación en esta fase):
 * - Sin engine OkHttp (ni pool, ni caché 50 MB en disco, ni proxy/auth).
 * - Sin `ContentEncoding` gzip/deflate (NSURLSession lo negocia solo).
 * - Sin cookies, visitorData ni login: búsqueda anónima.
 */
@OptIn(ExperimentalSerializationApi::class)
fun createYouTubeHttpClient(): HttpClient = HttpClient(platformHttpEngine()) {
    expectSuccess = false

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
            },
        )
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 60_000
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = 60_000
    }

    defaultRequest {
        url("https://music.youtube.com/youtubei/v1/")
        header("Accept", "application/json")
        header("Cache-Control", "no-cache")
    }
}
