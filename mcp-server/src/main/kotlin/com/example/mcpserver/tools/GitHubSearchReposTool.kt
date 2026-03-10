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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class GitHubSearchReposTool(
    private val gitHubClient: GitHubApiClient,
    private val json: Json
) {

    fun register(registry: ToolRegistry) {
        val schema = buildJsonObject {
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

        registry.register(
            name = "github_search_repos",
            description = "Search GitHub repositories by query. Returns name, description, stars, language, and URL for each repository.",
            inputSchema = schema,
            handler = this::execute
        )
    }

    private suspend fun execute(arguments: JsonObject?): McpCallToolResult {
        val query = arguments?.get("query")?.jsonPrimitive?.content
            ?: return McpCallToolResult(
                content = listOf(McpContent(text = "Missing required parameter: query")),
                isError = true
            )
        val maxResults = arguments["maxResults"]?.jsonPrimitive?.int ?: 5

        return try {
            val responseText = gitHubClient.get("/search/repositories?q=$query&per_page=$maxResults")
            val root = json.parseToJsonElement(responseText).jsonObject
            val items = root["items"]?.jsonArray ?: return McpCallToolResult(
                content = listOf(McpContent(text = "No results found")),
                isError = false
            )

            val text = buildString {
                appendLine("Found ${items.size} repositories for \"$query\":\n")
                items.forEach { item ->
                    val repo = item.jsonObject
                    val name = repo["full_name"]?.jsonPrimitive?.content ?: "unknown"
                    val desc = repo["description"]?.jsonPrimitive?.content ?: "No description"
                    val stars = repo["stargazers_count"]?.jsonPrimitive?.int ?: 0
                    val lang = repo["language"]?.jsonPrimitive?.content ?: "unknown"
                    val url = repo["html_url"]?.jsonPrimitive?.content ?: ""
                    appendLine("- **$name** ($lang, $stars stars)")
                    appendLine("  $desc")
                    appendLine("  $url")
                }
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
