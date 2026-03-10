package com.example.aichallengeapp.feature.chat.data.mcp

import com.example.aichallengeapp.core.mcp.McpConstants
import com.example.aichallengeapp.core.mcp.model.FunctionDefinition
import com.example.aichallengeapp.core.mcp.model.JsonRpcRequest
import com.example.aichallengeapp.core.mcp.model.JsonRpcResponse
import com.example.aichallengeapp.core.mcp.model.McpCallToolParams
import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import com.example.aichallengeapp.core.mcp.model.McpTool
import com.example.aichallengeapp.core.mcp.model.McpToolsListResult
import com.example.aichallengeapp.core.mcp.model.ToolDefinition
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

class McpToolClient(
    private val httpClient: HttpClient,
    private val mcpBaseUrl: String,
    private val json: Json
) {
    private val requestIdCounter = AtomicInteger(0)
    private var sessionId: String? = null
    private var cachedTools: List<McpTool>? = null
    private var initialized = false

    private fun nextRequestId() = requestIdCounter.incrementAndGet()

    suspend fun getToolDefinitions(): List<ToolDefinition> {
        val tools = getTools()
        return tools.map { tool ->
            ToolDefinition(
                function = FunctionDefinition(
                    name = tool.name,
                    description = tool.description,
                    parameters = tool.inputSchema
                )
            )
        }
    }

    private suspend fun getTools(): List<McpTool> {
        cachedTools?.let { return it }
        return try {
            ensureInitialized()
            val tools = listTools()
            cachedTools = tools
            tools
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to get MCP tools")
            emptyList()
        }
    }

    suspend fun callTool(name: String, arguments: JsonObject?): McpCallToolResult {
        return try {
            ensureInitialized()
            val callParams = McpCallToolParams(name = name, arguments = arguments)
            val request = JsonRpcRequest(
                id = nextRequestId(),
                method = "tools/call",
                params = json.encodeToJsonElement(callParams) as JsonObject
            )
            val response = httpClient.post(mcpBaseUrl) {
                contentType(ContentType.Application.Json)
                sessionId?.let { headers.append("Mcp-Session-Id", it) }
                setBody(request)
            }
            val rpcResponse: JsonRpcResponse = response.body()
            rpcResponse.error?.let { err ->
                return McpCallToolResult(
                    content = listOf(McpContent(text = "MCP error: ${err.message}")),
                    isError = true
                )
            }
            val resultElement = rpcResponse.result
                ?: return McpCallToolResult(
                    content = listOf(McpContent(text = "Empty result from MCP")),
                    isError = true
                )
            json.decodeFromJsonElement(McpCallToolResult.serializer(), resultElement)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to call tool $name")
            McpCallToolResult(
                content = listOf(McpContent(text = "Tool call failed: ${e.message}")),
                isError = true
            )
        }
    }

    private suspend fun ensureInitialized() {
        if (initialized) return
        val initRequest = JsonRpcRequest(
            id = nextRequestId(),
            method = "initialize",
            params = JsonObject(
                mapOf(
                    "protocolVersion" to JsonPrimitive(McpConstants.PROTOCOL_VERSION),
                    "capabilities" to JsonObject(emptyMap()),
                    "clientInfo" to JsonObject(
                        mapOf(
                            "name" to JsonPrimitive(McpConstants.CLIENT_NAME),
                            "version" to JsonPrimitive(McpConstants.CLIENT_VERSION)
                        )
                    )
                )
            )
        )
        val initResponse = httpClient.post(mcpBaseUrl) {
            contentType(ContentType.Application.Json)
            setBody(initRequest)
        }
        if (initResponse.status.isSuccess()) {
            sessionId = initResponse.headers["Mcp-Session-Id"]
            initialized = true

            // Send initialized notification
            httpClient.post(mcpBaseUrl) {
                contentType(ContentType.Application.Json)
                sessionId?.let { headers.append("Mcp-Session-Id", it) }
                setBody(JsonRpcRequest(method = "notifications/initialized"))
            }
        }
    }

    private suspend fun listTools(): List<McpTool> {
        val request = JsonRpcRequest(
            id = nextRequestId(),
            method = "tools/list"
        )
        val response = httpClient.post(mcpBaseUrl) {
            contentType(ContentType.Application.Json)
            sessionId?.let { headers.append("Mcp-Session-Id", it) }
            setBody(request)
        }
        val rpcResponse: JsonRpcResponse = response.body()
        val resultElement = rpcResponse.result ?: return emptyList()
        val toolsResult = json.decodeFromJsonElement(McpToolsListResult.serializer(), resultElement)
        return toolsResult.tools
    }

    companion object {
        private const val TAG = "McpToolClient"
    }
}
