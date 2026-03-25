package com.example.ragserver.query

import com.example.ragserver.ApiEndpoints
import com.example.ragserver.PromptTemplates
import com.example.ragserver.network.ChatRequest
import com.example.ragserver.network.ChatMessage
import com.example.ragserver.network.ChatResponse
import com.example.ragserver.network.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.slf4j.LoggerFactory

class DeepSeekQueryRewriter(
    private val apiKeyProvider: () -> String,
    private val client: HttpClient = createHttpClient()
) : QueryRewriter {

    private val log = LoggerFactory.getLogger(DeepSeekQueryRewriter::class.java)

    override suspend fun rewrite(query: String): String {
        log.info("[query-rewrite] using DeepSeek model=${ApiEndpoints.DEEPSEEK_MODEL}")
        return runCatching {
            val response = client.post(ApiEndpoints.DEEPSEEK_CHAT) {
                contentType(ContentType.Application.Json)
                bearerAuth(apiKeyProvider())
                setBody(
                    ChatRequest(
                        model = ApiEndpoints.DEEPSEEK_MODEL,
                        messages = listOf(
                            ChatMessage(role = "system", content = PromptTemplates.QUERY_REWRITE_SYSTEM),
                            ChatMessage(role = "user", content = query)
                        ),
                        maxTokens = MAX_TOKENS
                    )
                )
            }.body<ChatResponse>()
            response.choices.firstOrNull()?.message?.content?.trim() ?: query
        }.getOrDefault(query)
    }

    private companion object {
        const val MAX_TOKENS = 200
    }
}
