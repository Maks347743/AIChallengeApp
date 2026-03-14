package com.example.githubmcpserver.tools

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import com.example.githubmcpserver.github.GitHubApiClient
import com.example.githubmcpserver.github.GitHubAuthException
import com.example.githubmcpserver.github.GitHubNotFoundException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class GitHubGetUserTool(
    private val gitHubClient: GitHubApiClient,
    private val json: Json
) : GitHubMcpToolHandler {

    override val name = "github_get_user"

    override val description =
        "Get information about a GitHub user. Returns login, name, bio, public repos count, and followers count."

    override val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("username") {
                put("type", "string")
                put("description", "GitHub username to look up")
            }
        }
        putJsonArray("required") { add(JsonPrimitive("username")) }
    }

    override suspend fun execute(arguments: JsonObject?): McpCallToolResult {
        val username = arguments?.get("username")?.jsonPrimitive?.content
            ?: return McpCallToolResult(
                content = listOf(McpContent(text = "Missing required parameter: username")),
                isError = true
            )

        return try {
            val responseText = gitHubClient.get("/users/$username")
            val user = json.decodeFromString<GitHubUser>(responseText)

            val text = buildString {
                appendLine("GitHub User: **${user.login}**")
                appendLine("Name: ${user.name ?: "N/A"}")
                appendLine("Bio: ${user.bio ?: "No bio"}")
                appendLine("Public repos: ${user.publicRepos}")
                appendLine("Followers: ${user.followers}")
            }

            McpCallToolResult(content = listOf(McpContent(text = text)))
        } catch (_: GitHubNotFoundException) {
            McpCallToolResult(
                content = listOf(McpContent(text = "User not found: $username")),
                isError = true
            )
        } catch (e: GitHubAuthException) {
            McpCallToolResult(
                content = listOf(McpContent(text = "GitHub rate limit or auth error: ${e.message}")),
                isError = true
            )
        } catch (e: Exception) {
            McpCallToolResult(
                content = listOf(McpContent(text = "GitHub API error: ${e.message}")),
                isError = true
            )
        }
    }
}
