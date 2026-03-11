package com.example.aichallengeapp.feature.chat.data.tools

import com.example.aichallengeapp.core.periodictask.domain.repository.PeriodicTaskRepository
import com.example.aichallengeapp.core.mcp.model.FunctionDefinition
import com.example.aichallengeapp.core.mcp.model.ToolDefinition
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class ListPeriodicTasksTool(
    private val periodicTaskRepository: PeriodicTaskRepository
) : LocalToolHandler {

    override val isPeriodicTaskTool = true

    override val definition = ToolDefinition(
        function = FunctionDefinition(
            name = "list_periodic_tasks",
            description = "List all active periodic tasks for the current chat. The chat_id is provided automatically.",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonArray("required") {}
            }
        )
    )

    override suspend fun execute(arguments: JsonObject?, chatId: String): String {
        val tasks = periodicTaskRepository.getByChatId(chatId).filter { it.isActive }

        if (tasks.isEmpty()) return "No active periodic tasks for this chat."

        return buildString {
            appendLine("Active periodic tasks (${tasks.size}):")
            tasks.forEach { task ->
                appendLine("- ID: ${task.id}")
                appendLine("  Tool: ${task.toolName}")
                appendLine("  Interval: every ${task.intervalMinutes} minute(s)")
                appendLine("  Description: ${task.prompt}")
                val lastExec = task.lastExecutedAt
                if (lastExec != null) {
                    val format = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    appendLine("  Last executed: ${format.format(java.util.Date(lastExec))}")
                }
                appendLine()
            }
        }
    }
}
