package com.example.aichallengeapp.feature.chat.data.tools

import com.example.aichallengeapp.core.mcp.model.ToolDefinition
import kotlinx.serialization.json.JsonObject

interface LocalToolHandler {
    val definition: ToolDefinition
    val isPeriodicTaskTool: Boolean get() = false
    suspend fun execute(arguments: JsonObject?, chatId: String): String
}
