package com.example.supportmcpserver.tools

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import com.example.supportmcpserver.data.UserRepository
import com.example.supportmcpserver.mcp.SupportToolHandler
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

class GetUserInfoTool(private val userRepository: UserRepository) : SupportToolHandler {

    override val name = "get_user_info"

    override val description =
        "Look up a CRM user by user ID or email address. Returns account details including plan, status, and registration date. Use this when the user mentions their account, email, or ID."

    override val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("user_id") {
                put("type", "string")
                put("description", "The user's unique ID (e.g. usr-001)")
            }
            putJsonObject("email") {
                put("type", "string")
                put("description", "The user's email address")
            }
        }
    }

    override suspend fun execute(arguments: JsonObject?): McpCallToolResult {
        val userId = arguments?.get("user_id")?.jsonPrimitive?.contentOrNull
        val email = arguments?.get("email")?.jsonPrimitive?.contentOrNull

        if (userId == null && email == null) {
            return McpCallToolResult(
                content = listOf(McpContent(text = "Provide either user_id or email to look up a user.")),
                isError = true
            )
        }

        val user = when {
            userId != null -> userRepository.findById(userId)
            else -> userRepository.findByEmail(email!!)
        }

        if (user == null) {
            val lookup = userId ?: email
            return McpCallToolResult(
                content = listOf(McpContent(text = "No user found for: $lookup"))
            )
        }

        val text = buildString {
            appendLine("User: ${user.name} (${user.id})")
            appendLine("Email: ${user.email}")
            appendLine("Plan: ${user.plan}")
            appendLine("Status: ${user.status}")
            appendLine("Member since: ${user.createdAt}")
            appendLine("Location: ${user.location}")
        }
        return McpCallToolResult(content = listOf(McpContent(text = text.trim())))
    }
}
