package com.example.ragserver.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

const val TIMEOUT_EMBEDDING_MS = 120_000L
const val TIMEOUT_QUERY_REWRITE_MS = 60_000L
const val TIMEOUT_RERANK_MS = 120_000L

/** A single shared Json instance with common settings for all HTTP calls. */
val sharedJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

/** Creates a configured HttpClient. Each long-lived service that needs a different timeout gets its own instance via this factory. */
fun createHttpClient(timeoutMs: Long = TIMEOUT_EMBEDDING_MS): HttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(sharedJson)
    }
    engine {
        requestTimeout = timeoutMs
    }
}
