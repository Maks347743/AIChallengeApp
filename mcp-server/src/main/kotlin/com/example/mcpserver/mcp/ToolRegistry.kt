package com.example.mcpserver.mcp

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import com.example.aichallengeapp.core.mcp.model.McpTool
import com.example.mcpserver.tools.McpToolHandler
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

data class RegisteredTool(
    val name: String,
    val description: String,
    val inputSchema: JsonElement,
    val handler: McpToolHandler
)

class ToolRegistry {
    private val tools = mutableMapOf<String, RegisteredTool>()

    fun register(name: String, description: String, inputSchema: JsonElement, handler: McpToolHandler) {
        tools[name] = RegisteredTool(name, description, inputSchema, handler)
    }

    fun listTools(): List<McpTool> = tools.values.map { tool ->
        McpTool(
            name = tool.name,
            description = tool.description,
            inputSchema = tool.inputSchema
        )
    }

    suspend fun callTool(name: String, arguments: JsonObject?): McpCallToolResult {
        val tool = tools[name] ?: return McpCallToolResult(
            content = listOf(McpContent(text = "Unknown tool: $name")),
            isError = true
        )
        return tool.handler.execute(arguments)
    }
}
