package com.example.filesystemmcpserver.tools

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.nio.file.FileSystems

private const val MAX_ENTRIES = 300
private const val DEFAULT_MAX_DEPTH = 6

private val EXCLUDED_DIRS = setOf(
    "build", ".gradle", ".idea", ".git", ".kotlin",
    "node_modules", "__pycache__", ".DS_Store"
)

class ListFilesTool(private val projectDir: String) : FilesystemToolHandler {

    override val name = "list_files"

    override val description =
        "Lists files and directories within the project. Skips build/generated dirs (build, .gradle, .idea, .git) by default. Optionally filter by glob pattern or limit depth."

    override val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("path") {
                put("type", "string")
                put("description", "Relative path to the directory from the project root. Defaults to \".\" (project root).")
            }
            putJsonObject("pattern") {
                put("type", "string")
                put("description", "Optional glob pattern to filter results (e.g. **/*.kt).")
            }
            putJsonObject("max_depth") {
                put("type", "integer")
                put("description", "Maximum directory depth to traverse. Defaults to $DEFAULT_MAX_DEPTH.")
            }
        }
    }

    override suspend fun execute(arguments: JsonObject?): McpCallToolResult {
        val relativePath = (arguments?.get("path") as? JsonPrimitive)?.content ?: "."
        val pattern = (arguments?.get("pattern") as? JsonPrimitive)?.content
        val maxDepth = (arguments?.get("max_depth") as? JsonPrimitive)?.content?.toIntOrNull()
            ?: DEFAULT_MAX_DEPTH

        return withContext(Dispatchers.IO) {
            try {
                val root = File(projectDir).canonicalFile
                val startDir = sandboxedPath(projectDir, relativePath)

                if (!startDir.exists()) {
                    return@withContext McpCallToolResult(
                        content = listOf(McpContent(text = "Directory not found: $relativePath")),
                        isError = true
                    )
                }
                if (!startDir.isDirectory) {
                    return@withContext McpCallToolResult(
                        content = listOf(McpContent(text = "Not a directory: $relativePath")),
                        isError = true
                    )
                }

                val matcher = pattern?.let {
                    FileSystems.getDefault().getPathMatcher("glob:$it")
                }

                val entries = mutableListOf<String>()
                var truncated = false

                startDir.walkTopDown()
                    .onEnter { dir ->
                        val depth = dir.toRelativeString(startDir).split(File.separator).size
                        dir.name !in EXCLUDED_DIRS && depth <= maxDepth
                    }
                    .forEach { file ->
                        if (entries.size >= MAX_ENTRIES) {
                            truncated = true
                            return@forEach
                        }
                        val relative = file.toRelativeString(root)
                        if (matcher != null) {
                            val relPath = root.toPath().relativize(file.toPath())
                            if (matcher.matches(relPath)) entries.add(relative)
                        } else {
                            entries.add(relative)
                        }
                    }

                val result = buildString {
                    entries.forEach { appendLine(it) }
                    if (truncated) append("\n[...results truncated at $MAX_ENTRIES entries]")
                }.trimEnd()

                McpCallToolResult(content = listOf(McpContent(text = result)))
            } catch (e: IllegalArgumentException) {
                McpCallToolResult(
                    content = listOf(McpContent(text = e.message ?: "Invalid path")),
                    isError = true
                )
            } catch (e: Exception) {
                McpCallToolResult(
                    content = listOf(McpContent(text = "Error listing files: ${e.message}")),
                    isError = true
                )
            }
        }
    }
}
