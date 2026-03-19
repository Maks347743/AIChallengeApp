package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.ToolCallInfo
import com.example.aichallengeapp.feature.chat.data.mcp.McpToolClientManager
import com.example.aichallengeapp.feature.chat.data.tools.LocalToolRegistry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import timber.log.Timber

data class ChunkSourceInfo(
    val chunkId: String,
    val source: String,
    val section: String?,
    val textFragment: String
)

@Serializable
private data class ChunkMetaDto(
    val id: String,
    val source: String,
    val section: String? = null,
    val text: String
)

data class ToolExecutionResult(
    val messages: List<ChatMessage>,
    val hadPeriodicTaskTools: Boolean,
    val hadEmptyRetrieve: Boolean = false,
    val chunkSources: List<ChunkSourceInfo> = emptyList()
)

class ExecuteToolCallsUseCase(
    private val mcpToolClientManager: McpToolClientManager,
    private val localToolRegistry: LocalToolRegistry,
    private val json: Json
) {
    suspend operator fun invoke(toolCalls: List<ToolCallInfo>, chatId: String): ToolExecutionResult {
        var hadPeriodicTaskTools = false
        var hadEmptyRetrieve = false
        val chunkSources = mutableListOf<ChunkSourceInfo>()

        Timber.tag(TAG).d("Executing ${toolCalls.size} tool calls")

        val messages = toolCalls.map { toolCall ->
            val arguments = try {
                json.decodeFromString(JsonObject.serializer(), toolCall.arguments)
            } catch (_: Exception) {
                null
            }

            if (localToolRegistry.isPeriodicTaskTool(toolCall.functionName)) {
                hadPeriodicTaskTools = true
            }

            val isLocal = localToolRegistry.isLocalTool(toolCall.functionName)
            val isMcpRetrieve = !isLocal && toolCall.functionName == "retrieve"
            val serverLabel = if (isLocal) "local" else {
                val serverName = mcpToolClientManager.getServerName(toolCall.functionName)
                "mcp:${serverName ?: "unknown"}"
            }
            Timber.tag(TAG).d("▶ ${toolCall.functionName} [$serverLabel] | args=${toolCall.arguments.take(200)}")

            val resultText = try {
                if (isLocal) {
                    localToolRegistry.execute(toolCall.functionName, arguments, chatId)
                } else {
                    val result = mcpToolClientManager.callTool(toolCall.functionName, arguments)
                    if (isMcpRetrieve && result.isError == true) {
                        hadEmptyRetrieve = true
                    }
                    val metaText = result.content.firstOrNull { it.text?.startsWith("__RAG_META__:") == true }?.text
                    if (metaText != null) {
                        chunkSources += parseChunkMeta(metaText.removePrefix("__RAG_META__:"))
                    }
                    result.content
                        .filter { it.text?.startsWith("__RAG_META__:") != true }
                        .mapNotNull { it.text }
                        .joinToString("\n")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "✖ ${toolCall.functionName} FAILED")
                "Tool execution failed: ${e.message}"
            }

            val truncatedResult = resultText.take(MAX_TOOL_RESULT_CHARS)
            Timber.tag(TAG).d("✔ ${toolCall.functionName} | ${resultText.length} chars (truncated to ${truncatedResult.length}) | ${truncatedResult.take(150).replace("\n", " ")}")

            ChatMessage(
                role = ChatMessage.ROLE_TOOL_RESULT,
                content = truncatedResult,
                id = toolCall.id
            )
        }

        return ToolExecutionResult(messages, hadPeriodicTaskTools, hadEmptyRetrieve, chunkSources)
    }

    private fun parseChunkMeta(jsonStr: String): List<ChunkSourceInfo> {
        return try {
            json.decodeFromString<List<ChunkMetaDto>>(jsonStr).map { dto ->
                ChunkSourceInfo(
                    chunkId = dto.id,
                    source = dto.source,
                    section = dto.section,
                    textFragment = dto.text
                )
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to parse RAG chunk metadata")
            emptyList()
        }
    }

    companion object {
        private const val TAG = "ToolExecution"
        private const val MAX_TOOL_RESULT_CHARS = 2000
        const val CITATION_MARKER = "---ЦИТАТЫ---"
    }
}
