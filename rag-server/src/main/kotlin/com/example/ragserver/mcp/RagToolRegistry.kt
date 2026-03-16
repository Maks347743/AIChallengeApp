package com.example.ragserver.mcp

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import com.example.aichallengeapp.core.mcp.model.McpTool
import com.example.ragserver.mcp.tools.RetrieveTool
import kotlinx.serialization.json.JsonObject

class RagToolRegistry(private val retrieveTool: RetrieveTool) {

    fun listTools(): List<McpTool> = listOf(
        McpTool(
            name = retrieveTool.name,
            description = retrieveTool.description,
            inputSchema = retrieveTool.inputSchema
        )
    )

    suspend fun callTool(name: String, arguments: JsonObject?): McpCallToolResult {
        return when (name) {
            retrieveTool.name -> retrieveTool.execute(arguments)
            else -> McpCallToolResult(
                content = listOf(McpContent(text = "Unknown tool: $name")),
                isError = true
            )
        }
    }
}
