package com.example.githubmcpserver.tools

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class GitBranchTool(private val projectDir: String) : GitHubMcpToolHandler {

    override val name = "get_git_branch"

    override val description =
        "Returns the current git branch name of the project. Use this when the user asks about the current branch or git state."

    override val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {}
    }

    override suspend fun execute(arguments: JsonObject?): McpCallToolResult {
        return try {
            withContext(Dispatchers.IO) {
                val process = ProcessBuilder("git", "-C", projectDir, "branch", "--show-current")
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().readText().trim()
                process.waitFor()
                if (output.isEmpty()) {
                    McpCallToolResult(content = listOf(McpContent(text = "Could not determine branch (detached HEAD or not a git repo)")))
                } else {
                    McpCallToolResult(content = listOf(McpContent(text = "Current branch: $output")))
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
