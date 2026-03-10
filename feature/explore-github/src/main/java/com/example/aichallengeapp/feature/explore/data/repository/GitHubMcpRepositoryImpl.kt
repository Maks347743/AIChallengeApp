package com.example.aichallengeapp.feature.explore.data.repository

import com.example.aichallengeapp.core.mcp.McpConstants
import com.example.aichallengeapp.core.mcp.model.JsonRpcRequest
import com.example.aichallengeapp.core.mcp.model.JsonRpcResponse
import com.example.aichallengeapp.core.mcp.model.McpCallToolParams
import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpToolsListResult
import com.example.aichallengeapp.feature.explore.domain.GitHubMcpRepository
import com.example.aichallengeapp.feature.explore.domain.GitHubMcpResult
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

class GitHubMcpRepositoryImpl(
    private val httpClient: HttpClient,
    private val mcpBaseUrl: String,
    private val json: Json
) : GitHubMcpRepository {

    private val requestIdCounter = AtomicInteger(0)
    private var sessionId: String? = null

    private fun nextRequestId() = requestIdCounter.incrementAndGet()

    override suspend fun fetchTools(): GitHubMcpResult {
        return try {
            val tools = performMcpHandshake()
            GitHubMcpResult.Success(tools)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to fetch MCP tools")
            GitHubMcpResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun callTool(name: String, arguments: JsonObject?): McpCallToolResult {
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
            throw Exception("MCP error: ${err.message}")
        }
        val resultElement = rpcResponse.result
            ?: throw Exception("MCP tools/call returned null result")
        return json.decodeFromJsonElement(McpCallToolResult.serializer(), resultElement)
    }

    private suspend fun performMcpHandshake(): List<com.example.aichallengeapp.core.mcp.model.McpTool> {
        // Step 1: Initialize
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

        if (!initResponse.status.isSuccess()) {
            throw Exception("MCP initialize failed: ${initResponse.status}")
        }

        sessionId = initResponse.headers["Mcp-Session-Id"]
        Timber.tag(TAG).d("MCP session ID: $sessionId")

        val initRpc: JsonRpcResponse = initResponse.body()
        Timber.tag(TAG).d("MCP initialize result: ${initRpc.result}")

        // Step 2: Send initialized notification
        val notificationRequest = JsonRpcRequest(
            method = "notifications/initialized"
        )

        httpClient.post(mcpBaseUrl) {
            contentType(ContentType.Application.Json)
            sessionId?.let { headers.append("Mcp-Session-Id", it) }
            setBody(notificationRequest)
        }

        // Step 3: List tools
        val toolsRequest = JsonRpcRequest(
            id = nextRequestId(),
            method = "tools/list"
        )

        val toolsResponse = httpClient.post(mcpBaseUrl) {
            contentType(ContentType.Application.Json)
            sessionId?.let { headers.append("Mcp-Session-Id", it) }
            setBody(toolsRequest)
        }

        if (!toolsResponse.status.isSuccess()) {
            throw Exception("MCP tools/list failed: ${toolsResponse.status}")
        }

        val rpcResponse: JsonRpcResponse = toolsResponse.body()

        rpcResponse.error?.let { err ->
            throw Exception("MCP error: ${err.message} (code: ${err.code})")
        }

        val resultElement = rpcResponse.result
            ?: throw Exception("MCP tools/list returned null result")

        val toolsResult = json.decodeFromJsonElement(McpToolsListResult.serializer(), resultElement)
        return toolsResult.tools
    }

    private companion object {
        const val TAG = "GitHubMcpRepository"
    }
}
