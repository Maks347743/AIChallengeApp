package com.example.aichallengeapp.feature.chat.data.tools

import com.example.aichallengeapp.core.mcp.model.ToolDefinition
import kotlinx.serialization.json.JsonObject

class LocalToolRegistry(
    private val handlers: List<LocalToolHandler>
) {
    fun getToolDefinitions(): List<ToolDefinition> = handlers.map { it.definition }

    fun isLocalTool(name: String): Boolean = handlers.any { it.definition.function.name == name }

    fun isPeriodicTaskTool(name: String): Boolean =
        handlers.any { it.definition.function.name == name && it.isPeriodicTaskTool }

    suspend fun execute(name: String, arguments: JsonObject?, chatId: String): String {
        val handler = handlers.firstOrNull { it.definition.function.name == name }
            ?: return "Unknown local tool: $name"
        return handler.execute(arguments, chatId)
    }
}
