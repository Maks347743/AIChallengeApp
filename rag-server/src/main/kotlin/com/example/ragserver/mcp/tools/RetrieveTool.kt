package com.example.ragserver.mcp.tools

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import com.example.ragserver.config.RagConfig
import com.example.ragserver.data.Chunk
import com.example.ragserver.mcp.RetrievalPipeline
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

@Serializable
data class ChunkMeta(
    val id: String,
    val source: String,
    val section: String?,
    val text: String
)

class RetrieveTool(
    private val pipeline: RetrievalPipeline,
    private val configProvider: () -> RagConfig
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
                put("description", "Maximum results (default: server topK config)")
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
        val topK = arguments["maxResults"]?.jsonPrimitive?.int ?: configProvider().topK

        return try {
            val chunks = pipeline.retrieve(query, topK)
            when {
                chunks.isEmpty() -> McpCallToolResult(
                    content = listOf(McpContent(text = "No relevant documents found.")),
                    isError = true
                )
                else -> McpCallToolResult(content = listOf(
                    McpContent(text = formatChunks(chunks)),
                    McpContent(type = "metadata", text = "__RAG_META__:" + formatChunksMeta(chunks))
                ))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            McpCallToolResult(
                content = listOf(McpContent(text = "Retrieval error: ${e.message}")),
                isError = true
            )
        }
    }

    private fun formatChunks(chunks: List<Chunk>): String = buildString {
        chunks.forEachIndexed { i, chunk ->
            appendLine("[${i + 1}] ${chunk.metadata.title}")
            appendLine("Source: ${chunk.metadata.file} | Section: ${chunk.metadata.section ?: "General"} | strategy=${chunk.metadata.strategy} chunk#${chunk.metadata.chunkIndex}")
            appendLine(chunk.text)
            appendLine()
        }
    }.trim()

    private fun formatChunksMeta(chunks: List<Chunk>): String {
        val metas = chunks.map { chunk ->
            ChunkMeta(
                id = chunk.id,
                source = chunk.metadata.file,
                section = chunk.metadata.section,
                text = chunk.text.take(300)
            )
        }
        return Json.encodeToString(metas)
    }
}
