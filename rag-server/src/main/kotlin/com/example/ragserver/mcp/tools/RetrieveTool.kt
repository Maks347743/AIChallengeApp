package com.example.ragserver.mcp.tools

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import com.example.ragserver.data.ChunkStorage
import com.example.ragserver.data.VectorIndex
import com.example.ragserver.embedding.OllamaEmbeddingService
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.JsonPrimitive

class RetrieveTool(
    private val embeddingService: OllamaEmbeddingService,
    private val vectorIndex: VectorIndex,
    private val chunkStorage: ChunkStorage
) {
    val name = "retrieve"

    val description =
        "Search indexed documentation using semantic similarity. Call this when the user asks about installation, configuration, features, or any topic covered in stored project documentation."

    val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("query") {
                put("type", "string")
                put("description", "The search query")
            }
            putJsonObject("maxResults") {
                put("type", "integer")
                put("description", "Maximum results to return (default 3, max 3)")
            }
        }
        putJsonArray("required") { add(JsonPrimitive("query")) }
    }

    suspend fun execute(arguments: JsonObject?): McpCallToolResult {
        val query = arguments?.get("query")?.jsonPrimitive?.content
            ?: return McpCallToolResult(
                content = listOf(McpContent(text = "Missing required parameter: query")),
                isError = true
            )
        val maxResults = (arguments["maxResults"]?.jsonPrimitive?.int ?: 3).coerceAtMost(3)

        return try {
            val queryVec = embeddingService.embed(query)
                ?: return McpCallToolResult(
                    content = listOf(McpContent(text = "Failed to embed query: Ollama returned no vector.")),
                    isError = true
                )
            val chunkIds = vectorIndex.search(queryVec, maxResults)

            if (chunkIds.isEmpty()) {
                return McpCallToolResult(
                    content = listOf(
                        McpContent(text = "No relevant documents found in the index. Please add documents and run vectorization first.")
                    )
                )
            }

            val resultText = buildString {
                chunkIds.forEachIndexed { i, id ->
                    val chunk = chunkStorage.load(id)
                    if (chunk != null) {
                        appendLine("[${i + 1}] ${chunk.metadata.title}")
                        appendLine("Source: ${chunk.metadata.file} | Section: ${chunk.metadata.section ?: "General"} | strategy=${chunk.metadata.strategy} chunk#${chunk.metadata.chunkIndex}")
                        appendLine(chunk.text)
                        appendLine()
                    }
                }
            }.trim()

            McpCallToolResult(content = listOf(McpContent(text = resultText)))
        } catch (e: Exception) {
            McpCallToolResult(
                content = listOf(McpContent(text = "Retrieval error: ${e.message}")),
                isError = true
            )
        }
    }
}
