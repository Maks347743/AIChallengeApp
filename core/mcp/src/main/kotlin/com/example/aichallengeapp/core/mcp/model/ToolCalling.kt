package com.example.aichallengeapp.core.mcp.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ToolDefinition(
    val type: String = "function",
    val function: FunctionDefinition
)

@Serializable
data class FunctionDefinition(
    val name: String,
    val description: String? = null,
    val parameters: JsonElement? = null
)

@Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: FunctionCallDetail
)

@Serializable
data class FunctionCallDetail(
    val name: String,
    val arguments: String
)
