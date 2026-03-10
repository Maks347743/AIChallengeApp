package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.mcp.model.ToolDefinition
import com.example.aichallengeapp.feature.chat.data.mcp.McpToolClient

class GetToolDefinitionsUseCase(
    private val mcpToolClient: McpToolClient
) {
    suspend operator fun invoke(): List<ToolDefinition> = mcpToolClient.getToolDefinitions()
}
