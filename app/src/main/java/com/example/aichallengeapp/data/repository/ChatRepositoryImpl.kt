package com.example.aichallengeapp.data.repository

import com.example.aichallengeapp.data.model.ChatRequest
import com.example.aichallengeapp.data.model.ChatResponse
import com.example.aichallengeapp.data.model.DeepSeekDefaults
import com.example.aichallengeapp.data.model.MessageDto
import com.example.aichallengeapp.domain.repository.ChatRepository
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
    }

    override suspend fun sendMessage(message: String): Result<String> {
        return runCatching {
            val request = ChatRequest(
                messages = listOf(
                    MessageDto(
                        role = DeepSeekDefaults.ROLE_SYSTEM,
                        content = DeepSeekDefaults.SYSTEM_PROMPT
                    ),
                    MessageDto(
                        role = DeepSeekDefaults.ROLE_USER,
                        content = message
                    )
                )
            )
            val response: ChatResponse = httpClient.post("$baseUrl$CHAT_ENDPOINT") {
                contentType(ContentType.Application.Json)
                bearerAuth(apiKey)
                setBody(request)
            }.body()

            response.choices.firstOrNull()?.message?.content
                ?: error("Empty response from DeepSeek API")
        }
    }
}
