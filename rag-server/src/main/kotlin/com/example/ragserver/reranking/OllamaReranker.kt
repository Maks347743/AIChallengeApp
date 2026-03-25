package com.example.ragserver.reranking

import com.example.ragserver.ApiEndpoints
import com.example.ragserver.PromptTemplates
import com.example.ragserver.network.ChatMessage
import com.example.ragserver.network.OllamaChatRequest
import com.example.ragserver.network.OllamaChatResponse
import com.example.ragserver.network.TIMEOUT_RERANK_MS
import com.example.ragserver.network.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.slf4j.LoggerFactory

class OllamaReranker(
    private val baseUrlProvider: () -> String,
    private val modelProvider: () -> String,
    private val client: HttpClient = createHttpClient(timeoutMs = TIMEOUT_RERANK_MS)
) : Reranker {

    private val log = LoggerFactory.getLogger(OllamaReranker::class.java)

    override suspend fun rerank(query: String, documents: List<String>): List<Float> {
        if (documents.isEmpty()) return emptyList()
        val n = documents.size
        log.info("[rerank] using Ollama model=${modelProvider()} for $n documents")

        val numbered = documents.mapIndexed { i, doc ->
            "${i + 1}. ${doc.take(DOCUMENT_PREVIEW_CHARS)}"
        }.joinToString("\n\n")

        return runCatching {
            val response = client.post("${baseUrlProvider()}${ApiEndpoints.OLLAMA_NATIVE_CHAT_PATH}") {
                contentType(ContentType.Application.Json)
                setBody(
                    OllamaChatRequest(
                        model = modelProvider(),
                        messages = listOf(
                            ChatMessage(role = "system", content = PromptTemplates.RERANK_SYSTEM),
                            ChatMessage(role = "user", content = PromptTemplates.rerankUser(query, numbered, n))
                        ),
                        think = false
                    )
                )
            }.body<OllamaChatResponse>()

            val content = response.message.content.trim()
            log.info("[rerank] raw response: $content")

            val ranked = content.split(RANK_SPLIT_REGEX)
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..n }
                .distinct()

            log.info("[rerank] ranked order: $ranked")

            val scores = FloatArray(n) { 0f }
            ranked.forEachIndexed { position, docNumber ->
                scores[docNumber - 1] = (n - position).toFloat() / n
            }
            scores.toList()
        }.getOrDefault(FloatArray(n) { 0f }.toList())
    }

    private companion object {
        const val DOCUMENT_PREVIEW_CHARS = 300
        val RANK_SPLIT_REGEX = Regex("[,\\s]+")
    }
}
