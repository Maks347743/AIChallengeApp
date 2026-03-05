package com.example.aichallengeapp.feature.chat.data.repository

import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.ChatResult
import com.example.aichallengeapp.core.database.domain.model.ResponseMetrics
import com.example.aichallengeapp.core.database.domain.repository.ChatRepository
import com.example.aichallengeapp.feature.chat.data.model.ChatRequest
import com.example.aichallengeapp.feature.chat.data.model.ChatResponse
import com.example.aichallengeapp.feature.chat.data.model.MessageDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ChatRepositoryImpl(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val baseUrl: String
) : ChatRepository {

    companion object {
        private const val CHAT_ENDPOINT = "/chat/completions"
        // DeepSeek pricing: $0.28/1M input (cache miss), $0.42/1M output
        private const val PRICE_INPUT_PER_TOKEN = 0.28 / 1_000_000.0
        private const val PRICE_OUTPUT_PER_TOKEN = 0.42 / 1_000_000.0
    }

    override suspend fun sendMessage(
        messages: List<ChatMessage>,
        maxTokens: Int?,
        temperature: Float?,
        model: String
    ): Result<ChatResult> {
        return runCatching {
            val startTime = System.currentTimeMillis()
            val request = ChatRequest(
                model = model,
                messages = messages.map {
                    MessageDto(
                        role = when (it.role) {
                            ChatMessage.ROLE_SUMMARY -> ChatMessage.ROLE_USER
                            ChatMessage.ROLE_CONSTRAINT_VIOLATION_ASSISTANT -> ChatMessage.ROLE_ASSISTANT
                            ChatMessage.ROLE_CONSTRAINT_VIOLATION_USER -> ChatMessage.ROLE_USER
                            else -> it.role
                        },
                        content = it.content
                    )
                },
                maxTokens = maxTokens,
                temperature = temperature
            )
            val response: ChatResponse = httpClient.post("$baseUrl$CHAT_ENDPOINT") {
                contentType(ContentType.Application.Json)
                bearerAuth(apiKey)
                setBody(request)
            }.body()
            val responseTimeMs = System.currentTimeMillis() - startTime

            val message = response.choices.firstOrNull()?.message?.content
                ?: error("Empty response from DeepSeek API")
            val promptTokens = response.usage?.promptTokens ?: 0
            val completionTokens = response.usage?.completionTokens ?: 0
            val totalTokens = response.usage?.totalTokens ?: 0
            val costUsd = promptTokens * PRICE_INPUT_PER_TOKEN + completionTokens * PRICE_OUTPUT_PER_TOKEN

            ChatResult(
                message = message,
                metrics = ResponseMetrics(
                    responseTimeMs = responseTimeMs,
                    promptTokens = promptTokens,
                    completionTokens = completionTokens,
                    totalTokens = totalTokens,
                    costUsd = costUsd
                )
            )
        }
    }
}
