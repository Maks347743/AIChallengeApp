package com.example.aichallengeapp.feature.chat.data.tools

import com.example.aichallengeapp.core.database.domain.PeriodicTaskManager
import com.example.aichallengeapp.core.database.domain.repository.PeriodicTaskRepository
import com.example.aichallengeapp.core.mcp.model.FunctionDefinition
import com.example.aichallengeapp.core.mcp.model.ToolDefinition
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class StopPeriodicTaskTool(
    private val periodicTaskRepository: PeriodicTaskRepository,
    private val periodicTaskManager: PeriodicTaskManager
) : LocalToolHandler {

    override val definition = ToolDefinition(
        function = FunctionDefinition(
            name = "stop_periodic_task",
            description = "Stop a periodic task by its ID, or stop all periodic tasks for the current chat. Use this when the user asks to stop recurring updates. The chat_id is provided automatically.",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("task_id") {
                        put("type", "string")
                        put("description", "ID of the periodic task to stop. If not provided, stops all tasks for the current chat.")
                    }
                }
                putJsonArray("required") {}
            }
        )
    )

    override suspend fun execute(arguments: JsonObject?, chatId: String): String {
        val taskId = arguments?.get("task_id")?.jsonPrimitive?.content

        return if (taskId != null) {
            periodicTaskRepository.deactivate(taskId)
            periodicTaskManager.onTaskStopped()
            "Periodic task $taskId has been stopped."
        } else {
            periodicTaskRepository.deactivateByChatId(chatId)
            periodicTaskManager.onTaskStopped()
            "All periodic tasks for this chat have been stopped."
        }
    }
}
