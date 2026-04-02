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
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.nio.file.FileSystems

private const val MAX_MATCHES = 200
private const val MAX_FILE_SIZE_BYTES = 1_048_576L // 1 MB

private val BINARY_EXTENSIONS = setOf(
    "class", "jar", "png", "jpg", "jpeg", "gif", "bmp", "webp",
    "so", "bin", "exe", "dll", "zip", "tar", "gz", "aar", "apk",
    "keystore", "jks", "pdf", "mp3", "mp4", "wav", "ogg"
)

class SearchInFilesTool(private val projectDir: String) : FilesystemToolHandler {

    override val name = "search_in_files"

    override val description =
        "Searches for a text query in file contents. Returns matching lines with file path and line number."

    override val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("query") {
                put("type", "string")
                put("description", "Text to search for (case-insensitive).")
            }
            putJsonObject("path") {
                put("type", "string")
                put("description", "Relative path to search within. Defaults to \".\" (project root).")
            }
            putJsonObject("filePattern") {
                put("type", "string")
                put("description", "Optional glob pattern applied to filenames (e.g. *.kt).")
            }
        }
        putJsonArray("required") {
            add(JsonPrimitive("query"))
        }
    }

    override suspend fun execute(arguments: JsonObject?): McpCallToolResult {
        val query = (arguments?.get("query") as? JsonPrimitive)?.content
            ?: return McpCallToolResult(
                content = listOf(McpContent(text = "Missing required argument: query")),
                isError = true
            )

        val relativePath = (arguments.get("path") as? JsonPrimitive)?.content ?: "."
        val filePattern = (arguments.get("filePattern") as? JsonPrimitive)?.content

        return withContext(Dispatchers.IO) {
            try {
                val root = File(projectDir).canonicalFile
                val startDir = sandboxedPath(projectDir, relativePath)

                if (!startDir.exists()) {
                    return@withContext McpCallToolResult(
                        content = listOf(McpContent(text = "Path not found: $relativePath")),
                        isError = true
                    )
                }

                val filenameMatcher = filePattern?.let {
                    FileSystems.getDefault().getPathMatcher("glob:$it")
                }

                val queryLower = query.lowercase()
                val matches = mutableListOf<String>()
                var truncated = false

                val files = if (startDir.isFile) listOf(startDir).asSequence() else startDir.walkTopDown().filter { it.isFile }

                for (file in files) {
                    if (matches.size >= MAX_MATCHES) {
                        truncated = true
                        break
                    }

                    val ext = file.extension.lowercase()
                    if (ext in BINARY_EXTENSIONS) continue
                    if (file.length() > MAX_FILE_SIZE_BYTES) continue

                    if (filenameMatcher != null) {
                        val fileNamePath = file.toPath().fileName
                        if (!filenameMatcher.matches(fileNamePath)) continue
                    }

                    val relPath = file.toRelativeString(root)

                    try {
                        file.bufferedReader().use { reader ->
                            var lineNumber = 0
                            reader.forEachLine { line ->
                                lineNumber++
                                if (matches.size < MAX_MATCHES && line.lowercase().contains(queryLower)) {
                                    matches.add("$relPath:$lineNumber: $line")
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Skip files that can't be read as text
                    }
                }

                val result = buildString {
                    if (matches.isEmpty()) {
                        append("No matches found for \"$query\".")
                    } else {
                        matches.forEach { appendLine(it) }
                        if (truncated) append("[...results truncated at $MAX_MATCHES matches]")
                    }
                }.trimEnd()

                McpCallToolResult(content = listOf(McpContent(text = result)))
            } catch (e: IllegalArgumentException) {
                McpCallToolResult(
                    content = listOf(McpContent(text = e.message ?: "Invalid path")),
                    isError = true
                )
            } catch (e: Exception) {
                McpCallToolResult(
                    content = listOf(McpContent(text = "Error searching files: ${e.message}")),
                    isError = true
                )
            }
        }
    }
}
