package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.ToolCallInfo
import com.example.aichallengeapp.feature.chat.data.mcp.McpToolClientManager
import com.example.aichallengeapp.feature.chat.data.tools.LocalToolRegistry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import timber.log.Timber

data class ToolExecutionResult(
    val messages: List<ChatMessage>,
    val hadPeriodicTaskTools: Boolean
)

class ExecuteToolCallsUseCase(
    private val mcpToolClientManager: McpToolClientManager,
    private val localToolRegistry: LocalToolRegistry,
    private val json: Json
) {
    suspend operator fun invoke(toolCalls: List<ToolCallInfo>, chatId: String): ToolExecutionResult {
        var hadPeriodicTaskTools = false
        Timber.tag(TAG).d("Executing ${toolCalls.size} tool calls")

        val messages = toolCalls.map { toolCall ->
            val arguments = try {
                json.decodeFromString(JsonObject.serializer(), toolCall.arguments)
            } catch (_: Exception) {
                null
            }

            if (localToolRegistry.isPeriodicTaskTool(toolCall.functionName)) {
                hadPeriodicTaskTools = true
            }

            val isLocal = localToolRegistry.isLocalTool(toolCall.functionName)
            val serverLabel = if (isLocal) "local" else {
                val serverName = mcpToolClientManager.getServerName(toolCall.functionName)
                "mcp:${serverName ?: "unknown"}"
            }
            Timber.tag(TAG).d("▶ ${toolCall.functionName} [$serverLabel] | args=${toolCall.arguments.take(200)}")

            val resultText = try {
                if (isLocal) {
                    localToolRegistry.execute(toolCall.functionName, arguments, chatId)
                } else {
                    val result = mcpToolClientManager.callTool(toolCall.functionName, arguments)
                    result.content.mapNotNull { it.text }.joinToString("\n")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "✖ ${toolCall.functionName} FAILED")
                "Tool execution failed: ${e.message}"
            }

            val truncatedResult = resultText.take(MAX_TOOL_RESULT_CHARS)
            Timber.tag(TAG).d("✔ ${toolCall.functionName} | ${resultText.length} chars (truncated to ${truncatedResult.length}) | ${truncatedResult.take(150).replace("\n", " ")}")

            ChatMessage(
                role = ChatMessage.ROLE_TOOL_RESULT,
                content = truncatedResult,
                id = toolCall.id
            )
        }

        return ToolExecutionResult(messages, hadPeriodicTaskTools)
    }

    companion object {
        private const val TAG = "ToolExecution"
        private const val MAX_TOOL_RESULT_CHARS = 2000
    }
}
