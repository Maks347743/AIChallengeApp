package com.example.githubmcpserver.tools

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

private const val MAX_DIFF_CHARS = 8000

class GitDiffTool(private val projectDir: String) : GitHubMcpToolHandler {

    override val name = "get_git_diff"

    override val description =
        "Returns the current git diff of the project (staged + unstaged changes). " +
        "Use staged_only=true to see only staged (cached) changes. " +
        "Use path to filter diff to a specific module or file (e.g. 'feature/chat'). " +
        "Use this when the user asks about current changes, what was modified, or the diff."

    override val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("staged_only") {
                put("type", "boolean")
                put("description", "If true, show only staged (cached) changes. Default: false (shows all changes)")
            }
            putJsonObject("path") {
                put("type", "string")
                put("description", "Optional path filter (e.g. 'feature/chat') to limit diff to a specific module or file")
            }
        }
    }

    override suspend fun execute(arguments: JsonObject?): McpCallToolResult {
        val stagedOnly = arguments?.get("staged_only")?.jsonPrimitive?.boolean ?: false
        val path = arguments?.get("path")?.jsonPrimitive?.contentOrNull
        val baseArgs = if (stagedOnly) {
            listOf("git", "-C", projectDir, "diff", "--cached")
        } else {
            listOf("git", "-C", projectDir, "diff", "HEAD")
        }
        val gitArgs = if (path != null) baseArgs + listOf("--", path) else baseArgs
        return try {
            withContext(Dispatchers.IO) {
                val process = ProcessBuilder(gitArgs)
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()

                val truncated = if (output.length > MAX_DIFF_CHARS) {
                    output.take(MAX_DIFF_CHARS) + "\n\n[...diff truncated at $MAX_DIFF_CHARS chars]"
                } else {
                    output
                }

                if (truncated.isBlank()) {
                    McpCallToolResult(content = listOf(McpContent(text = "No changes detected.")))
                } else {
                    McpCallToolResult(content = listOf(McpContent(text = truncated)))
                }
            }
        } catch (e: Exception) {
            McpCallToolResult(
                content = listOf(McpContent(text = "Git error: ${e.message}")),
                isError = true
            )
        }
    }
}
