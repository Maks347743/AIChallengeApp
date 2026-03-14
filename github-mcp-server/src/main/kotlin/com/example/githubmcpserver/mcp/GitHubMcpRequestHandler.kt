package com.example.githubmcpserver.mcp

import com.example.aichallengeapp.core.mcp.McpConstants
import com.example.aichallengeapp.core.mcp.model.JsonRpcError
import com.example.aichallengeapp.core.mcp.model.JsonRpcRequest
import com.example.aichallengeapp.core.mcp.model.JsonRpcResponse
import com.example.aichallengeapp.core.mcp.model.McpCallToolParams
import com.example.aichallengeapp.core.mcp.model.McpInitializeResult
import com.example.aichallengeapp.core.mcp.model.McpServerInfo
import com.example.aichallengeapp.core.mcp.model.McpToolsListResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

class GitHubMcpRequestHandler(private val registry: GitHubToolRegistry) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun handle(request: JsonRpcRequest): JsonRpcResponse {
        return when (request.method) {
            "initialize" -> handleInitialize(request)
            "tools/list" -> handleToolsList(request)
            "tools/call" -> handleToolsCall(request)
            else -> JsonRpcResponse(
                id = request.id?.let { JsonPrimitive(it) },
                error = JsonRpcError(
                    code = -32601,
                    message = "Method not found: ${request.method}"
                )
            )
        }
    }

    private fun handleInitialize(request: JsonRpcRequest): JsonRpcResponse {
        val result = McpInitializeResult(
            protocolVersion = McpConstants.PROTOCOL_VERSION,
            capabilities = JsonObject(emptyMap()),
            serverInfo = McpServerInfo(
                name = McpConstants.SERVER_NAME,
                version = McpConstants.SERVER_VERSION
            )
        )
        return JsonRpcResponse(
            id = request.id?.let { JsonPrimitive(it) },
            result = json.encodeToJsonElement(result)
        )
    }

    private fun handleToolsList(request: JsonRpcRequest): JsonRpcResponse {
        val result = McpToolsListResult(tools = registry.listTools())
        return JsonRpcResponse(
            id = request.id?.let { JsonPrimitive(it) },
            result = json.encodeToJsonElement(result)
        )
    }

    private suspend fun handleToolsCall(request: JsonRpcRequest): JsonRpcResponse {
        val params = request.params?.let {
            json.decodeFromJsonElement(McpCallToolParams.serializer(), it)
        } ?: return JsonRpcResponse(
            id = request.id?.let { JsonPrimitive(it) },
            error = JsonRpcError(code = -32602, message = "Missing params for tools/call")
        )

        return try {
            val result = registry.callTool(params.name, params.arguments)
            JsonRpcResponse(
                id = request.id?.let { JsonPrimitive(it) },
                result = json.encodeToJsonElement(result)
            )
        } catch (e: Exception) {
            JsonRpcResponse(
                id = request.id?.let { JsonPrimitive(it) },
                error = JsonRpcError(code = -32000, message = e.message ?: "Tool execution failed")
            )
        }
    }
}
