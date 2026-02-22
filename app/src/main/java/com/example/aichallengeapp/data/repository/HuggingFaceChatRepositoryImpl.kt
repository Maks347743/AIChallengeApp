// TEMPORARY: This file implements HuggingFace Inference API support.
// To delete HF support: remove this file, HuggingFaceRoutingChatRepository.kt,
// and the HF sections in AppModule.kt and build.gradle.kts.
package com.example.aichallengeapp.data.repository

import com.example.aichallengeapp.data.model.ChatRequest
import com.example.aichallengeapp.data.model.ChatResponse
import com.example.aichallengeapp.data.model.MessageDto
import com.example.aichallengeapp.domain.model.ChatMessage
import com.example.aichallengeapp.domain.model.ChatResult
import com.example.aichallengeapp.domain.model.ResponseMetrics
import com.example.aichallengeapp.domain.repository.ChatRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class HuggingFaceChatRepositoryImpl(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val baseUrl: String
) : ChatRepository {

    companion object {
        // :cheapest lets HF router auto-select the cheapest available provider (free quota included)
        private const val HF_MODEL_ID = "deepseek-ai/DeepSeek-R1-Distill-Qwen-7B:cheapest"
        private const val CHAT_ENDPOINT = "/chat/completions"
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
                model = HF_MODEL_ID,
                messages = messages.map { MessageDto(role = it.role, content = it.content) },
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
                ?: error("Empty response from HuggingFace API")
            val promptTokens = response.usage?.promptTokens ?: 0
            val completionTokens = response.usage?.completionTokens ?: 0
            val totalTokens = response.usage?.totalTokens ?: 0

            ChatResult(
                message = message,
                metrics = ResponseMetrics(
                    responseTimeMs = responseTimeMs,
                    promptTokens = promptTokens,
                    completionTokens = completionTokens,
                    totalTokens = totalTokens,
                    costUsd = 0.0 // HuggingFace free tier
                )
            )
        }
    }
}
