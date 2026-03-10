package com.example.aichallengeapp.feature.explore.data.repository

import com.example.aichallengeapp.feature.explore.data.model.JsonRpcRequest
import com.example.aichallengeapp.feature.explore.data.model.JsonRpcResponse
import com.example.aichallengeapp.feature.explore.data.model.McpToolsListResult
import com.example.aichallengeapp.feature.explore.domain.GitHubMcpRepository
import com.example.aichallengeapp.feature.explore.domain.GitHubMcpResult
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

class GitHubMcpRepositoryImpl(
    private val httpClient: HttpClient,
    private val githubKey: String,
    private val mcpBaseUrl: String,
    private val json: Json
) : GitHubMcpRepository {

    private val requestIdCounter = AtomicInteger(0)

    private fun nextRequestId() = requestIdCounter.incrementAndGet()

    private suspend fun extractJsonFromSse(response: HttpResponse): String {
        val raw = response.bodyAsText()
        val events = raw.split(Regex("\n\n+"))
        val dataLines = events
            .flatMap { event -> event.lines().filter { it.startsWith("data:") } }
            .map { it.removePrefix("data:").trim() }
            .filter { it.isNotEmpty() && it != "[DONE]" }
        return if (dataLines.isNotEmpty()) dataLines.first() else raw
    }

    private suspend fun parseRpcResponse(response: HttpResponse): JsonRpcResponse {
        val jsonStr = extractJsonFromSse(response)
        return json.decodeFromString(JsonRpcResponse.serializer(), jsonStr)
    }

    override suspend fun fetchTools(): GitHubMcpResult {
        return try {
            val tools = performMcpHandshake()
            GitHubMcpResult.Success(tools)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to fetch MCP tools")
            when {
                e.message?.contains("Authentication failed") == true ->
                    GitHubMcpResult.AuthError(e.message ?: "Authentication failed")
                else ->
                    GitHubMcpResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun performMcpHandshake(): List<com.example.aichallengeapp.feature.explore.data.model.McpTool> {
        // Step 1: Initialize
        val initRequest = JsonRpcRequest(
            id = nextRequestId(),
            method = "initialize",
            params = JsonObject(
                mapOf(
                    "protocolVersion" to JsonPrimitive(MCP_PROTOCOL_VERSION),
                    "capabilities" to JsonObject(emptyMap()),
                    "clientInfo" to JsonObject(
                        mapOf(
                            "name" to JsonPrimitive(CLIENT_NAME),
                            "version" to JsonPrimitive(CLIENT_VERSION)
                        )
                    )
                )
            )
        )

        val initResponse = httpClient.post(mcpBaseUrl) {
            contentType(ContentType.Application.Json)
            bearerAuth(githubKey)
            setBody(initRequest)
        }

        if (!initResponse.status.isSuccess()) {
            val errorBody = initResponse.bodyAsText()
            Timber.tag(TAG).e("MCP initialize failed: ${initResponse.status} - $errorBody")
            if (initResponse.status.value == 401) {
                throw Exception("Authentication failed. Check your GitHub Key.")
            }
            throw Exception("MCP initialize failed: ${initResponse.status}")
        }

        val sessionId = initResponse.headers["Mcp-Session-Id"]
        Timber.tag(TAG).d("MCP session ID: $sessionId")

        val initRpc = parseRpcResponse(initResponse)
        Timber.tag(TAG).d("MCP initialize result: ${initRpc.result}")

        // Step 2: Send initialized notification (no id = notification)
        val notificationRequest = JsonRpcRequest(
            method = "notifications/initialized"
        )

        val notifResponse = httpClient.post(mcpBaseUrl) {
            contentType(ContentType.Application.Json)
            bearerAuth(githubKey)
            sessionId?.let { headers.append("Mcp-Session-Id", it) }
            setBody(notificationRequest)
        }

        if (!notifResponse.status.isSuccess()) {
            Timber.tag(TAG).w("MCP notification response: ${notifResponse.status}")
        }

        // Step 3: List tools
        val toolsRequest = JsonRpcRequest(
            id = nextRequestId(),
            method = "tools/list"
        )

        val toolsResponse = httpClient.post(mcpBaseUrl) {
            contentType(ContentType.Application.Json)
            bearerAuth(githubKey)
            sessionId?.let { headers.append("Mcp-Session-Id", it) }
            setBody(toolsRequest)
        }

        if (!toolsResponse.status.isSuccess()) {
            val errorBody = toolsResponse.bodyAsText()
            Timber.tag(TAG).e("MCP tools/list failed: ${toolsResponse.status} - $errorBody")
            if (toolsResponse.status.value == 401) {
                throw Exception("Authentication failed. Check your GitHub Key.")
            }
            throw Exception("MCP tools/list failed: ${toolsResponse.status}")
        }

        val rpcResponse = parseRpcResponse(toolsResponse)

        if (rpcResponse.error != null) {
            throw Exception("MCP error: ${rpcResponse.error.message} (code: ${rpcResponse.error.code})")
        }

        val resultElement = rpcResponse.result
            ?: throw Exception("MCP tools/list returned null result")

        val toolsResult = json.decodeFromJsonElement(McpToolsListResult.serializer(), resultElement)
        return toolsResult.tools
    }

    private companion object {
        const val TAG = "GitHubMcpRepository"
        const val MCP_PROTOCOL_VERSION = "2025-03-26"
        const val CLIENT_NAME = "AIChallengeApp"
        const val CLIENT_VERSION = "1.0.0"
    }
}
