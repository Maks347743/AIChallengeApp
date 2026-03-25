package com.example.ragserver.query

import com.example.ragserver.ApiEndpoints
import com.example.ragserver.PromptTemplates
import com.example.ragserver.network.ChatMessage
import com.example.ragserver.network.OllamaChatRequest
import com.example.ragserver.network.OllamaChatResponse
import com.example.ragserver.network.TIMEOUT_QUERY_REWRITE_MS
import com.example.ragserver.network.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.slf4j.LoggerFactory

class OllamaQueryRewriter(
    private val baseUrlProvider: () -> String,
    private val modelProvider: () -> String,
    private val client: HttpClient = createHttpClient(timeoutMs = TIMEOUT_QUERY_REWRITE_MS)
) : QueryRewriter {

    private val log = LoggerFactory.getLogger(OllamaQueryRewriter::class.java)

    override suspend fun rewrite(query: String): String {
        log.info("[query-rewrite] using Ollama model=${modelProvider()}")
        return runCatching {
            val response = client.post("${baseUrlProvider()}${ApiEndpoints.OLLAMA_NATIVE_CHAT_PATH}") {
                contentType(ContentType.Application.Json)
                setBody(
                    OllamaChatRequest(
                        model = modelProvider(),
                        messages = listOf(
                            ChatMessage(role = "system", content = PromptTemplates.QUERY_REWRITE_SYSTEM),
                            ChatMessage(role = "user", content = query)
                        ),
                        think = false
                    )
                )
            }.body<OllamaChatResponse>()
            response.message.content.trim().ifBlank { query }
        }.getOrDefault(query)
    }
}
