package com.example.githubmcpserver.tools

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import com.example.githubmcpserver.github.GitHubApiClient
import com.example.githubmcpserver.github.GitHubAuthException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class GitHubSearchReposTool(
    private val gitHubClient: GitHubApiClient,
    private val json: Json
) : GitHubMcpToolHandler {

    override val name = "github_search_repos"

    override val description =
        "Search GitHub repositories by query. Returns name, description, stars, language, and URL for each repository."

    override val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("query") {
                put("type", "string")
                put("description", "Search query for GitHub repositories")
            }
            putJsonObject("maxResults") {
                put("type", "integer")
                put("description", "Maximum number of results to return (default 5)")
            }
        }
        putJsonArray("required") { add(JsonPrimitive("query")) }
    }

    override suspend fun execute(arguments: JsonObject?): McpCallToolResult {
        val query = arguments?.get("query")?.jsonPrimitive?.content
            ?: return McpCallToolResult(
                content = listOf(McpContent(text = "Missing required parameter: query")),
                isError = true
            )
        val maxResults = arguments["maxResults"]?.jsonPrimitive?.int ?: 5

        return try {
            val responseText = gitHubClient.get("/search/repositories?q=$query&per_page=$maxResults")
            val response = json.decodeFromString<GitHubSearchResponse>(responseText)

            if (response.items.isEmpty()) {
                return McpCallToolResult(
                    content = listOf(McpContent(text = "No results found")),
                    isError = false
                )
            }

            val text = buildString {
                appendLine("Found ${response.items.size} repositories for \"$query\":\n")
                response.items.forEach { repo ->
                    appendLine("- **${repo.fullName}** (${repo.language ?: "unknown"}, ${repo.stars} stars)")
                    appendLine("  ${repo.description ?: "No description"}")
                    appendLine("  ${repo.htmlUrl}")
                }
            }

            McpCallToolResult(content = listOf(McpContent(text = text)))
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
