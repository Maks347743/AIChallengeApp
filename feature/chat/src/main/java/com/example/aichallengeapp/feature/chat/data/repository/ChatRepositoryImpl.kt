package com.example.aichallengeapp.feature.chat.data.repository

import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.ChatResult
import com.example.aichallengeapp.core.database.domain.model.ResponseMetrics
import com.example.aichallengeapp.core.database.domain.model.ToolCallInfo
import com.example.aichallengeapp.core.database.domain.repository.ChatRepository
import com.example.aichallengeapp.core.mcp.model.FunctionCallDetail
import com.example.aichallengeapp.core.mcp.model.ToolCall
import com.example.aichallengeapp.core.mcp.model.ToolDefinition
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
import kotlinx.serialization.json.Json

class ChatRepositoryImpl(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val baseUrl: String,
    private val json: Json
) : ChatRepository {

    companion object {
        private const val CHAT_ENDPOINT = "/chat/completions"
        private const val PRICE_INPUT_PER_TOKEN = 0.28 / 1_000_000.0
        private const val PRICE_OUTPUT_PER_TOKEN = 0.42 / 1_000_000.0
    }

    override suspend fun sendMessage(
        messages: List<ChatMessage>,
        maxTokens: Int?,
        temperature: Float?,
        model: String,
        tools: List<ToolDefinition>?
    ): Result<ChatResult> {
        return runCatching {
            val startTime = System.currentTimeMillis()
            val request = ChatRequest(
                model = model,
                messages = messages.map { msg -> mapToDto(msg) },
                maxTokens = maxTokens,
                temperature = temperature,
                tools = tools?.ifEmpty { null }
            )
            val response: ChatResponse = httpClient.post("$baseUrl$CHAT_ENDPOINT") {
                contentType(ContentType.Application.Json)
                bearerAuth(apiKey)
                setBody(request)
            }.body()
            val responseTimeMs = System.currentTimeMillis() - startTime

            val choice = response.choices.firstOrNull()
            val message = choice?.message?.content ?: ""
            val finishReason = choice?.finishReason
            val promptTokens = response.usage?.promptTokens ?: 0
            val completionTokens = response.usage?.completionTokens ?: 0
            val totalTokens = response.usage?.totalTokens ?: 0
            val costUsd = promptTokens * PRICE_INPUT_PER_TOKEN + completionTokens * PRICE_OUTPUT_PER_TOKEN

            val toolCalls = choice?.message?.toolCalls?.map { tc ->
                ToolCallInfo(
                    id = tc.id,
                    functionName = tc.function.name,
                    arguments = tc.function.arguments
                )
            }

            ChatResult(
                message = message,
                metrics = ResponseMetrics(
                    responseTimeMs = responseTimeMs,
                    promptTokens = promptTokens,
                    completionTokens = completionTokens,
                    totalTokens = totalTokens,
                    costUsd = costUsd
                ),
                toolCalls = toolCalls,
                finishReason = finishReason
            )
        }
    }

    private fun mapToDto(msg: ChatMessage): MessageDto {
        return when (msg.role) {
            ChatMessage.ROLE_TOOL_CALL -> {
                // Content contains serialized List<ToolCallInfo> — reconstruct tool_calls
                val toolCallInfos = try {
                    json.decodeFromString<List<ToolCallInfo>>(msg.content)
                } catch (_: Exception) {
                    emptyList()
                }
                val toolCalls = toolCallInfos.map { info ->
                    ToolCall(
                        id = info.id,
                        function = FunctionCallDetail(
                            name = info.functionName,
                            arguments = info.arguments
                        )
                    )
                }
                MessageDto(
                    role = ChatMessage.ROLE_ASSISTANT,
                    content = null,
                    toolCalls = toolCalls.ifEmpty { null }
                )
            }
            ChatMessage.ROLE_TOOL_RESULT -> {
                MessageDto(
                    role = "tool",
                    content = msg.content,
                    toolCallId = msg.id
                )
            }
            else -> {
                MessageDto(
                    role = when (msg.role) {
                        ChatMessage.ROLE_SUMMARY,
                        ChatMessage.ROLE_FACTS,
                        ChatMessage.ROLE_CONSTRAINT_VIOLATION_USER -> ChatMessage.ROLE_USER
                        ChatMessage.ROLE_CONSTRAINT_VIOLATION_ASSISTANT -> ChatMessage.ROLE_ASSISTANT
                        else -> msg.role
                    },
                    content = msg.content
                )
            }
        }
    }
}
