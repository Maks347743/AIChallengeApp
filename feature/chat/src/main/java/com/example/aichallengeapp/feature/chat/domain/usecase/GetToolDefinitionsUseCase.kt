package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.mcp.model.ToolDefinition
import com.example.aichallengeapp.feature.chat.data.mcp.McpToolClient
import com.example.aichallengeapp.feature.chat.data.tools.LocalToolRegistry

class GetToolDefinitionsUseCase(
    private val mcpToolClient: McpToolClient,
    private val localToolRegistry: LocalToolRegistry
) {
    suspend operator fun invoke(): List<ToolDefinition> {
        val mcpTools = mcpToolClient.getToolDefinitions()
        val localTools = localToolRegistry.getToolDefinitions()
        return mcpTools + localTools
    }
}
