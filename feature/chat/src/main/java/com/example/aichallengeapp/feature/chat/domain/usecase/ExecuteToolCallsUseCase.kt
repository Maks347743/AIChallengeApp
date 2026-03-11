package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.ToolCallInfo
import com.example.aichallengeapp.feature.chat.data.mcp.McpToolClient
import com.example.aichallengeapp.feature.chat.data.tools.LocalToolRegistry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class ExecuteToolCallsUseCase(
    private val mcpToolClient: McpToolClient,
    private val localToolRegistry: LocalToolRegistry,
    private val json: Json
) {
    suspend operator fun invoke(toolCalls: List<ToolCallInfo>, chatId: String): List<ChatMessage> {
        return toolCalls.map { toolCall ->
            val arguments = try {
                json.decodeFromString(JsonObject.serializer(), toolCall.arguments)
            } catch (_: Exception) {
                null
            }

            val resultText = if (localToolRegistry.isLocalTool(toolCall.functionName)) {
                localToolRegistry.execute(toolCall.functionName, arguments, chatId)
            } else {
                val result = mcpToolClient.callTool(toolCall.functionName, arguments)
                result.content.mapNotNull { it.text }.joinToString("\n")
            }

            ChatMessage(
                role = ChatMessage.ROLE_TOOL_RESULT,
                content = resultText,
                id = toolCall.id
            )
        }
    }
}
