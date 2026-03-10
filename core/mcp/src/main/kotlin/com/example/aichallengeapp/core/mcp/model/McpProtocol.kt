package com.example.aichallengeapp.core.mcp.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class McpTool(
    val name: String,
    val description: String? = null,
    val inputSchema: JsonElement? = null
)

@Serializable
data class McpToolsListResult(
    val tools: List<McpTool>
)

@Serializable
data class McpCallToolParams(
    val name: String,
    val arguments: JsonObject? = null
)

@Serializable
data class McpCallToolResult(
    val content: List<McpContent>,
    val isError: Boolean? = null
)

@Serializable
data class McpContent(
    val type: String = "text",
    val text: String? = null
)

@Serializable
data class McpInitializeResult(
    val protocolVersion: String,
    val capabilities: JsonObject? = null,
    val serverInfo: McpServerInfo
)

@Serializable
data class McpServerInfo(
    val name: String,
    val version: String
)
