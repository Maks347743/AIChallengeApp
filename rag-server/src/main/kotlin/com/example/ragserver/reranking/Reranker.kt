package com.example.ragserver.reranking

import com.example.ragserver.network.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class Reranker(
    private val apiKeyProvider: () -> String,
    private val client: HttpClient = createHttpClient()
) {

    /**
     * Returns relevance scores in the same order as [documents].
     * Returns emptyList() on failure (fail-safe).
     */
    suspend fun rerank(query: String, documents: List<String>): List<Float> {
        if (documents.isEmpty()) return emptyList()
        return runCatching {
            val response = client.post("https://api.jina.ai/v1/rerank") {
                contentType(ContentType.Application.Json)
                bearerAuth(apiKeyProvider())
                setBody(
                    Request(
                        model = "jina-reranker-v2-base-multilingual",
                        query = query,
                        documents = documents,
                        topN = documents.size
                    )
                )
            }.body<Response>()

            // Restore scores in original document order
            val scores = FloatArray(documents.size) { 0f }
            response.results.forEach { result ->
                scores[result.index] = result.relevanceScore
            }
            scores.toList()
        }.getOrDefault(emptyList())
    }

    @Serializable
    private data class Request(
        val model: String,
        val query: String,
        val documents: List<String>,
        @SerialName("top_n") val topN: Int
    )

    @Serializable
    private data class Response(val results: List<Result> = emptyList())

    @Serializable
    private data class Result(
        val index: Int,
        @SerialName("relevance_score") val relevanceScore: Float
    )
}
