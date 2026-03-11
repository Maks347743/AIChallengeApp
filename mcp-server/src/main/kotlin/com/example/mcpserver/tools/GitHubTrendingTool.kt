package com.example.mcpserver.tools

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import com.example.mcpserver.github.GitHubApiClient
import com.example.mcpserver.mcp.ToolRegistry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GitHubTrendingTool(
    private val gitHubClient: GitHubApiClient,
    private val json: Json
) {

    fun register(registry: ToolRegistry) {
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("language") {
                    put("type", "string")
                    put("description", "Programming language to filter by (e.g. kotlin, python, javascript). Optional.")
                }
                putJsonObject("period") {
                    put("type", "string")
                    put("description", "Time period: daily, weekly, or monthly. Default: daily")
                }
                putJsonObject("maxResults") {
                    put("type", "integer")
                    put("description", "Maximum number of results to return (default 10)")
                }
            }
            putJsonArray("required") {}
        }

        registry.register(
            name = "github_trending",
            description = "Get trending GitHub repositories. Shows the most starred repositories created within a given time period, optionally filtered by programming language.",
            inputSchema = schema,
            handler = this::execute
        )
    }

    private suspend fun execute(arguments: JsonObject?): McpCallToolResult {
        val language = arguments?.get("language")?.jsonPrimitive?.content
        val period = arguments?.get("period")?.jsonPrimitive?.content ?: "daily"
        val maxResults = arguments?.get("maxResults")?.jsonPrimitive?.int ?: 10

        val sinceDate = when (period) {
            "weekly" -> LocalDate.now().minusWeeks(1)
            "monthly" -> LocalDate.now().minusMonths(1)
            else -> LocalDate.now().minusDays(1)
        }
        val dateStr = sinceDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        val query = buildString {
            append("created:>$dateStr")
            if (!language.isNullOrBlank()) {
                append("+language:$language")
            }
        }

        return try {
            val responseText = gitHubClient.get("/search/repositories?q=$query&sort=stars&order=desc&per_page=$maxResults")
            val root = json.parseToJsonElement(responseText).jsonObject
            val items = root["items"]?.jsonArray ?: return McpCallToolResult(
                content = listOf(McpContent(text = "No trending repositories found")),
                isError = false
            )

            val text = buildString {
                val langLabel = if (!language.isNullOrBlank()) " ($language)" else ""
                appendLine("Trending repositories$langLabel for $period period (since $dateStr):\n")
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
