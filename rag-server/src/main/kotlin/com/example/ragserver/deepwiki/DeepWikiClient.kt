package com.example.ragserver.deepwiki

import com.example.aichallengeapp.core.mcp.McpConstants
import com.example.aichallengeapp.core.mcp.model.JsonRpcRequest
import com.example.aichallengeapp.core.mcp.model.JsonRpcResponse
import com.example.aichallengeapp.core.mcp.model.McpCallToolParams
import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import java.util.concurrent.atomic.AtomicInteger

class DeepWikiClient(
    private val baseUrl: String = "https://mcp.deepwiki.com/mcp"
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
        engine { requestTimeout = 60_000 }
    }
    private val idCounter = AtomicInteger(0)
    private var sessionId: String? = null
    private var initialized = false

    private fun nextId() = idCounter.incrementAndGet()

    suspend fun initialize() {
        if (initialized) return
        val request = JsonRpcRequest(
            id = nextId(),
            method = "initialize",
            params = buildJsonObject {
                put("protocolVersion", McpConstants.PROTOCOL_VERSION)
                put("capabilities", JsonObject(emptyMap()))
                put("clientInfo", buildJsonObject {
                    put("name", McpConstants.CLIENT_NAME)
                    put("version", McpConstants.CLIENT_VERSION)
                })
            }
        )
        val response = client.post(baseUrl) {
            contentType(ContentType.Application.Json)
            headers.append(HttpHeaders.Accept, "application/json, text/event-stream")
            setBody(request)
        }
        if (response.status.isSuccess()) {
            sessionId = response.headers["Mcp-Session-Id"]
            initialized = true
            // Send initialized notification
            client.post(baseUrl) {
                contentType(ContentType.Application.Json)
                headers.append(HttpHeaders.Accept, "application/json, text/event-stream")
                sessionId?.let { headers.append("Mcp-Session-Id", it) }
                setBody(JsonRpcRequest(method = "notifications/initialized"))
            }
        }
    }

    suspend fun callTool(name: String, arguments: JsonObject): McpCallToolResult {
        initialize()
        val params = McpCallToolParams(name = name, arguments = arguments)
        val request = JsonRpcRequest(
            id = nextId(),
            method = "tools/call",
            params = json.encodeToJsonElement(params) as JsonObject
        )
        val rpc = doPost(request)
        rpc.error?.let {
            return McpCallToolResult(
                content = listOf(com.example.aichallengeapp.core.mcp.model.McpContent(text = "Error: ${it.message}")),
                isError = true
            )
        }
        val result = rpc.result ?: return McpCallToolResult(
            content = listOf(com.example.aichallengeapp.core.mcp.model.McpContent(text = "Empty response")),
            isError = true
        )
        return json.decodeFromJsonElement(McpCallToolResult.serializer(), result)
    }

    private suspend fun doPost(request: JsonRpcRequest): JsonRpcResponse {
        val response = client.post(baseUrl) {
            contentType(ContentType.Application.Json)
            headers.append(HttpHeaders.Accept, "application/json, text/event-stream")
            sessionId?.let { headers.append("Mcp-Session-Id", it) }
            setBody(request)
        }
        val ct = response.contentType()
        return if (ct?.match(ContentType.Text.EventStream) == true) {
            val text = response.bodyAsText()
            val dataLine = text.lineSequence()
                .filter { it.startsWith("data:") }
                .map { it.removePrefix("data:").trim() }
                .lastOrNull { it.isNotEmpty() }
                ?: error("SSE response contained no data lines")
            json.decodeFromString(JsonRpcResponse.serializer(), dataLine)
        } else {
            response.body()
        }
    }
}
