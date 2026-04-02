package com.example.filesystemmcpserver.tools

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.FileNotFoundException
import java.nio.charset.MalformedInputException

private const val MAX_CHARS = 50_000
private const val TRUNCATED_SUFFIX = "\n[...truncated]"

class ReadFileTool(private val projectDir: String) : FilesystemToolHandler {

    override val name = "read_file"

    override val description =
        "Reads the full text content of a file within the project directory."

    override val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("path") {
                put("type", "string")
                put("description", "Relative path to the file from the project root.")
            }
        }
        putJsonArray("required") {
            add(kotlinx.serialization.json.JsonPrimitive("path"))
        }
    }

    override suspend fun execute(arguments: JsonObject?): McpCallToolResult {
        val path = arguments?.get("path")
            ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
            ?: return McpCallToolResult(
                content = listOf(McpContent(text = "Missing required argument: path")),
                isError = true
            )

        return withContext(Dispatchers.IO) {
            try {
                val file = sandboxedPath(projectDir, path)
                if (!file.exists()) {
                    return@withContext McpCallToolResult(
                        content = listOf(McpContent(text = "File not found: $path")),
                        isError = true
                    )
                }
                if (!file.isFile) {
                    return@withContext McpCallToolResult(
                        content = listOf(McpContent(text = "Not a file: $path")),
                        isError = true
                    )
                }
                val text = file.readText()
                val result = if (text.length > MAX_CHARS) {
                    text.take(MAX_CHARS) + TRUNCATED_SUFFIX
                } else {
                    text
                }
                McpCallToolResult(content = listOf(McpContent(text = result)))
            } catch (e: MalformedInputException) {
                McpCallToolResult(
                    content = listOf(McpContent(text = "Binary file, cannot read as text.")),
                    isError = true
                )
            } catch (e: FileNotFoundException) {
                McpCallToolResult(
                    content = listOf(McpContent(text = "File not found: $path")),
                    isError = true
                )
            } catch (e: IllegalArgumentException) {
                McpCallToolResult(
                    content = listOf(McpContent(text = e.message ?: "Invalid path")),
                    isError = true
                )
            } catch (e: Exception) {
                McpCallToolResult(
                    content = listOf(McpContent(text = "Error reading file: ${e.message}")),
                    isError = true
                )
            }
        }
    }
}
