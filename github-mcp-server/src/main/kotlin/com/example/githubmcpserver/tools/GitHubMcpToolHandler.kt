package com.example.githubmcpserver.tools

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

interface GitHubMcpToolHandler {
    val name: String
    val description: String
    val inputSchema: JsonElement
    suspend fun execute(arguments: JsonObject?): McpCallToolResult
}
