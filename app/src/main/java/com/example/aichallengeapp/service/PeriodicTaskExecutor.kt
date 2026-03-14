package com.example.aichallengeapp.service

import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.periodictask.domain.model.PeriodicTask
import com.example.aichallengeapp.core.periodictask.domain.model.PeriodicTaskMessage
import com.example.aichallengeapp.core.periodictask.domain.model.PeriodicTaskResult
import com.example.aichallengeapp.core.database.domain.repository.ChatRepository
import com.example.aichallengeapp.core.periodictask.domain.repository.PeriodicTaskRepository
import com.example.aichallengeapp.feature.chat.data.mcp.McpToolClientManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import timber.log.Timber
import java.util.UUID

class PeriodicTaskExecutor(
    private val mcpToolClientManager: McpToolClientManager,
    private val periodicTaskRepository: PeriodicTaskRepository,
    private val chatRepository: ChatRepository,
    private val json: Json,
    private val summarizationModel: String
) {

    suspend fun execute(task: PeriodicTask): PeriodicTaskMessage? {
        return try {
            val arguments = try {
                json.decodeFromString(JsonObject.serializer(), task.toolArgumentsJson)
            } catch (_: Exception) {
                null
            }

            val toolResult = mcpToolClientManager.callTool(task.toolName, arguments)
            val resultText = toolResult.content.mapNotNull { it.text }.joinToString("\n")

            val summary = summarize(task, resultText)

            val now = System.currentTimeMillis()
            periodicTaskRepository.updateLastExecuted(task.id, now)
            periodicTaskRepository.insertResult(
                PeriodicTaskResult(
                    id = UUID.randomUUID().toString(),
                    taskId = task.id,
                    result = resultText,
                    summary = summary,
                    createdAt = now
                )
            )

            PeriodicTaskMessage(
                chatId = task.chatId,
                taskId = task.id,
                summary = summary,
                toolName = task.toolName,
                timestamp = now
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to execute periodic task ${task.id}")
            null
        }
    }

    private suspend fun summarize(task: PeriodicTask, rawResult: String): String {
        return try {
            val messages = listOf(
                ChatMessage(
                    role = ChatMessage.ROLE_SYSTEM,
                    content = SUMMARIZER_SYSTEM_PROMPT
                ),
                ChatMessage(
                    role = ChatMessage.ROLE_USER,
                    content = "Задача: ${task.prompt}\n\nРезультат инструмента '${task.toolName}':\n$rawResult"
                )
            )
            val result = chatRepository.sendMessage(
                messages = messages,
                maxTokens = SUMMARIZER_MAX_TOKENS,
                temperature = SUMMARIZER_TEMPERATURE,
                model = summarizationModel
            )
            result.getOrNull()?.message ?: rawResult.take(FALLBACK_MAX_LENGTH)
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Summarization failed, using raw result")
            rawResult.take(FALLBACK_MAX_LENGTH)
        }
    }

    companion object {
        private const val TAG = "PeriodicTaskExecutor"
        private const val SUMMARIZER_SYSTEM_PROMPT =
            "You are a concise summarizer. Summarize the following tool result in 2-3 sentences in Russian. Focus on the most important/interesting items."
        private const val SUMMARIZER_MAX_TOKENS = 300
        private const val SUMMARIZER_TEMPERATURE = 0.3f
        private const val FALLBACK_MAX_LENGTH = 500
    }
}
