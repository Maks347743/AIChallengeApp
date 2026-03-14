package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.mcp.model.ToolDefinition
import com.example.aichallengeapp.feature.chat.data.mcp.McpToolClientManager
import com.example.aichallengeapp.feature.chat.data.tools.LocalToolRegistry

data class ToolDefinitionsResult(
    val all: List<ToolDefinition>,
    val mcpCount: Int
)

class GetToolDefinitionsUseCase(
    private val mcpToolClientManager: McpToolClientManager,
    private val localToolRegistry: LocalToolRegistry
) {
    suspend operator fun invoke(): ToolDefinitionsResult {
        val mcpTools = mcpToolClientManager.getToolDefinitions()
        val localTools = localToolRegistry.getToolDefinitions()
        return ToolDefinitionsResult(
            all = mcpTools + localTools,
            mcpCount = mcpTools.size
        )
    }
}
