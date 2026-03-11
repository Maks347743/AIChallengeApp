package com.example.aichallengeapp.feature.chat.data.tools

import com.example.aichallengeapp.core.database.domain.PeriodicTaskManager
import com.example.aichallengeapp.core.database.domain.model.PeriodicTask
import com.example.aichallengeapp.core.database.domain.repository.PeriodicTaskRepository
import com.example.aichallengeapp.core.mcp.model.FunctionDefinition
import com.example.aichallengeapp.core.mcp.model.ToolDefinition
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.UUID

class CreatePeriodicTaskTool(
    private val periodicTaskRepository: PeriodicTaskRepository,
    private val periodicTaskManager: PeriodicTaskManager
) : LocalToolHandler {

    override val definition = ToolDefinition(
        function = FunctionDefinition(
            name = "create_periodic_task",
            description = "Create a periodic task that executes an MCP tool at a specified interval and posts summarized results to the chat. Use this when the user asks for recurring information (e.g., 'show GitHub trends every 3 minutes'). The chat_id is provided automatically.",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("tool_name") {
                        put("type", "string")
                        put("description", "Name of the MCP tool to execute periodically (e.g. github_trending)")
                    }
                    putJsonObject("tool_arguments") {
                        put("type", "string")
                        put("description", "JSON string of arguments to pass to the tool")
                    }
                    putJsonObject("interval_minutes") {
                        put("type", "integer")
                        put("description", "Interval in minutes between executions")
                    }
                    putJsonObject("prompt") {
                        put("type", "string")
                        put("description", "Description of what this periodic task does, shown to the user")
                    }
                }
                putJsonArray("required") {
                    add(JsonPrimitive("tool_name"))
                    add(JsonPrimitive("interval_minutes"))
                    add(JsonPrimitive("prompt"))
                }
            }
        )
    )

    override suspend fun execute(arguments: JsonObject?, chatId: String): String {
        if (arguments == null) return "Missing arguments for create_periodic_task"

        val toolName = arguments["tool_name"]?.jsonPrimitive?.content
            ?: return "Missing required parameter: tool_name"
        val toolArguments = arguments["tool_arguments"]?.jsonPrimitive?.content ?: "{}"
        val intervalMinutes = arguments["interval_minutes"]?.jsonPrimitive?.int
            ?: return "Missing required parameter: interval_minutes"
        val prompt = arguments["prompt"]?.jsonPrimitive?.content
            ?: return "Missing required parameter: prompt"

        if (intervalMinutes < 1) return "Interval must be at least 1 minute"

        val task = PeriodicTask(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            toolName = toolName,
            toolArgumentsJson = toolArguments,
            intervalMinutes = intervalMinutes,
            prompt = prompt
        )

        periodicTaskRepository.upsert(task)
        periodicTaskManager.onTaskCreated()

        return "Periodic task created successfully. Task ID: ${task.id}. " +
                "Tool '$toolName' will be executed every $intervalMinutes minute(s). " +
                "Description: $prompt"
    }
}
