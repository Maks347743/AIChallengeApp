package com.example.mcpserver.tools

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import com.example.mcpserver.github.GitHubApiClient
import com.example.mcpserver.mcp.ToolRegistry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class GitHubGetUserTool(
    private val gitHubClient: GitHubApiClient,
    private val json: Json
) {

    fun register(registry: ToolRegistry) {
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("username") {
                    put("type", "string")
                    put("description", "GitHub username to look up")
                }
            }
            putJsonArray("required") { add(JsonPrimitive("username")) }
        }

        registry.register(
            name = "github_get_user",
            description = "Get information about a GitHub user. Returns login, name, bio, public repos count, and followers count.",
            inputSchema = schema,
            handler = this::execute
        )
    }

    private suspend fun execute(arguments: JsonObject?): McpCallToolResult {
        val username = arguments?.get("username")?.jsonPrimitive?.content
            ?: return McpCallToolResult(
                content = listOf(McpContent(text = "Missing required parameter: username")),
                isError = true
            )

        return try {
            val responseText = gitHubClient.get("/users/$username")
            val user = json.parseToJsonElement(responseText).jsonObject

            val login = user["login"]?.jsonPrimitive?.content ?: "unknown"
            val name = user["name"]?.jsonPrimitive?.content ?: "N/A"
            val bio = user["bio"]?.jsonPrimitive?.content ?: "No bio"
            val publicRepos = user["public_repos"]?.jsonPrimitive?.int ?: 0
            val followers = user["followers"]?.jsonPrimitive?.int ?: 0

            val text = buildString {
                appendLine("GitHub User: **$login**")
                appendLine("Name: $name")
                appendLine("Bio: $bio")
                appendLine("Public repos: $publicRepos")
                appendLine("Followers: $followers")
            }

            McpCallToolResult(content = listOf(McpContent(text = text)))
        } catch (e: Exception) {
            McpCallToolResult(
                content = listOf(McpContent(text = "GitHub API error: ${e.message}")),
                isError = true
            )
        }
    }
}
