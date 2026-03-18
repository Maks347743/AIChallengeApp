package com.example.ragserver.query

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

class QueryRewriter(
    private val apiKeyProvider: () -> String,
    private val client: HttpClient = createHttpClient()
) {

    suspend fun rewrite(query: String): String {
        return runCatching {
            val response = client.post("https://api.deepseek.com/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                bearerAuth(apiKeyProvider())
                setBody(
                    Request(
                        model = "deepseek-chat",
                        messages = listOf(
                            Message(
                                role = "system",
                                content = "Rewrite the user's search query to be more detailed and semantically rich for embedding-based document retrieval. Output only the rewritten query, nothing else."
                            ),
                            Message(role = "user", content = query)
                        ),
                        maxTokens = 200
                    )
                )
            }.body<Response>()
            response.choices.firstOrNull()?.message?.content?.trim() ?: query
        }.getOrDefault(query)
    }

    @Serializable
    private data class Request(
        val model: String,
        val messages: List<Message>,
        @SerialName("max_tokens") val maxTokens: Int = 200
    )

    @Serializable
    private data class Message(val role: String, val content: String)

    @Serializable
    private data class Response(val choices: List<Choice> = emptyList())

    @Serializable
    private data class Choice(val message: Message)
}
