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

class CreateFileTool(private val projectDir: String) : FilesystemToolHandler {

    override val name = "create_file"

    override val description =
        "Creates a new file with the given content. Fails if the file already exists."

    override val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("path") {
                put("type", "string")
                put("description", "Relative path to the new file from the project root.")
            }
            putJsonObject("content") {
                put("type", "string")
                put("description", "Content to write to the new file.")
            }
        }
        putJsonArray("required") {
            add(JsonPrimitive("path"))
            add(JsonPrimitive("content"))
        }
    }

    override suspend fun execute(arguments: JsonObject?): McpCallToolResult {
        val path = (arguments?.get("path") as? JsonPrimitive)?.content
            ?: return McpCallToolResult(
                content = listOf(McpContent(text = "Missing required argument: path")),
                isError = true
            )
        val content = (arguments.get("content") as? JsonPrimitive)?.content
            ?: return McpCallToolResult(
                content = listOf(McpContent(text = "Missing required argument: content")),
                isError = true
            )

        return withContext(Dispatchers.IO) {
            try {
                val file = sandboxedPath(projectDir, path)

                if (file.exists()) {
                    return@withContext McpCallToolResult(
                        content = listOf(McpContent(text = "File already exists: $path")),
                        isError = true
                    )
                }

                file.parentFile?.mkdirs()
                file.writeText(content)

                McpCallToolResult(
                    content = listOf(McpContent(text = "File created: $path (${content.length} bytes)"))
                )
            } catch (e: IllegalArgumentException) {
                McpCallToolResult(
                    content = listOf(McpContent(text = e.message ?: "Invalid path")),
                    isError = true
                )
            } catch (e: Exception) {
                McpCallToolResult(
                    content = listOf(McpContent(text = "Error creating file: ${e.message}")),
                    isError = true
                )
            }
        }
    }
}
