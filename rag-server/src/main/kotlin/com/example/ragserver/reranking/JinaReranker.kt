package com.example.ragserver.reranking

import com.example.ragserver.ApiEndpoints
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
import org.slf4j.LoggerFactory

class JinaReranker(
    private val apiKeyProvider: () -> String,
    private val client: HttpClient = createHttpClient()
) : Reranker {

    private val log = LoggerFactory.getLogger(JinaReranker::class.java)

    override suspend fun rerank(query: String, documents: List<String>): List<Float> {
        if (documents.isEmpty()) return emptyList()
        log.info("[rerank] using Jina AI model=${ApiEndpoints.JINA_RERANK_MODEL} for ${documents.size} documents")
        return runCatching {
            val response = client.post(ApiEndpoints.JINA_RERANK) {
                contentType(ContentType.Application.Json)
                bearerAuth(apiKeyProvider())
                setBody(
                    Request(
                        model = ApiEndpoints.JINA_RERANK_MODEL,
                        query = query,
                        documents = documents,
                        topN = documents.size
                    )
                )
            }.body<Response>()

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
