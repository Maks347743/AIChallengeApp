package com.example.aichallengeapp.feature.explore.domain

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import kotlinx.serialization.json.JsonObject

interface GitHubMcpRepository {
    suspend fun fetchTools(): GitHubMcpResult
    suspend fun callTool(name: String, arguments: JsonObject?): McpCallToolResult
}
