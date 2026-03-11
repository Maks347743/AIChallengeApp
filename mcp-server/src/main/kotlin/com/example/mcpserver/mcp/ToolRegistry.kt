package com.example.mcpserver.mcp

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import com.example.aichallengeapp.core.mcp.model.McpTool
import com.example.mcpserver.tools.McpToolHandler
import kotlinx.serialization.json.JsonObject

class ToolRegistry(tools: List<McpToolHandler>) {
    private val toolMap = tools.associateBy { it.name }

    fun listTools(): List<McpTool> = toolMap.values.map { tool ->
        McpTool(
            name = tool.name,
            description = tool.description,
            inputSchema = tool.inputSchema
        )
    }

    suspend fun callTool(name: String, arguments: JsonObject?): McpCallToolResult {
        val tool = toolMap[name] ?: return McpCallToolResult(
            content = listOf(McpContent(text = "Unknown tool: $name")),
            isError = true
        )
        return tool.execute(arguments)
    }
}
