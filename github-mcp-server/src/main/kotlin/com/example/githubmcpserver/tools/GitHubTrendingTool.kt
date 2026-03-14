package com.example.githubmcpserver.tools

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import com.example.githubmcpserver.github.GitHubApiClient
import com.example.githubmcpserver.github.GitHubAuthException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GitHubTrendingTool(
    private val gitHubClient: GitHubApiClient,
    private val json: Json
) : GitHubMcpToolHandler {

    override val name = "github_trending"

    override val description =
        "Get trending GitHub repositories. Shows the most starred repositories created within a given time period, optionally filtered by programming language."

    override val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("language") {
                put("type", "string")
                put("description", "Programming language to filter by (e.g. kotlin, python, javascript). Optional.")
            }
            putJsonObject("period") {
                put("type", "string")
                put("description", "Time period: daily, weekly, monthly, yearly, or all_time. Default: daily")
            }
            putJsonObject("maxResults") {
                put("type", "integer")
                put("description", "Maximum number of results to return (default 10)")
            }
        }
        putJsonArray("required") {}
    }

    override suspend fun execute(arguments: JsonObject?): McpCallToolResult {
        val language = arguments?.get("language")?.jsonPrimitive?.content
        val period = arguments?.get("period")?.jsonPrimitive?.content ?: "daily"
        val maxResults = arguments?.get("maxResults")?.jsonPrimitive?.int ?: 10

        val sinceDate = when (period) {
            "weekly" -> LocalDate.now().minusWeeks(1)
            "monthly" -> LocalDate.now().minusMonths(1)
            "yearly" -> LocalDate.now().minusYears(1)
            "all_time" -> null
            else -> LocalDate.now().minusDays(1)
        }

        val query = buildString {
            if (sinceDate != null) {
                append("created:>${sinceDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
            } else {
                append("stars:>1000")
            }
            if (!language.isNullOrBlank()) {
                append(" language:$language")
            }
        }

        return try {
            val responseText = gitHubClient.get("/search/repositories?q=$query&sort=stars&order=desc&per_page=$maxResults")
            val response = json.decodeFromString<GitHubSearchResponse>(responseText)

            if (response.items.isEmpty()) {
                return McpCallToolResult(
                    content = listOf(McpContent(text = "No trending repositories found")),
                    isError = false
                )
            }

            val text = buildString {
                val langLabel = if (!language.isNullOrBlank()) " ($language)" else ""
                val periodLabel = if (sinceDate != null) "$period period (since ${sinceDate.format(DateTimeFormatter.ISO_LOCAL_DATE)})" else "all time"
                appendLine("Trending repositories$langLabel for $periodLabel:\n")
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
