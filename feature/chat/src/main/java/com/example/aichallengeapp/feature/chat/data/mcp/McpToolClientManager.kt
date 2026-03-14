package com.example.aichallengeapp.feature.chat.data.mcp

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import com.example.aichallengeapp.core.mcp.model.ToolDefinition
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import timber.log.Timber

data class McpServerConfig(
    val name: String,
    val client: McpToolClient
)

class McpToolClientManager(private val servers: List<McpServerConfig>) {
    private val initMutex = Mutex()
    private var toolToServer: Map<String, McpServerConfig>? = null

    private suspend fun ensureInitialized() {
        if (toolToServer != null) return
        initMutex.withLock {
            if (toolToServer != null) return
            val mapping = mutableMapOf<String, McpServerConfig>()
            for (server in servers) {
                try {
                    val tools = server.client.getToolDefinitions()
                    val toolNames = tools.map { it.function.name }
                    for (name in toolNames) {
                        mapping[name] = server
                        Timber.tag(TAG).i("  ${server.name} → $name")
                    }
                    Timber.tag(TAG).i("  ► ${server.name}: ${toolNames.size} tools loaded")
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Failed to initialize server '${server.name}' — skipping")
                }
            }
            Timber.tag(TAG).i("MCP Servers initialized: ${mapping.size} tools from ${servers.size} servers")
            toolToServer = mapping
        }
    }

    suspend fun getToolDefinitions(): List<ToolDefinition> {
        ensureInitialized()
        val allTools = mutableListOf<ToolDefinition>()
        val mapping = toolToServer ?: return emptyList()
        val processedServers = mutableSetOf<String>()
        for ((_, server) in mapping) {
            if (server.name in processedServers) continue
            processedServers.add(server.name)
            try {
                allTools.addAll(server.client.getToolDefinitions())
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to get tools from '${server.name}'")
            }
        }
        return allTools
    }

    suspend fun callTool(name: String, arguments: JsonObject?): McpCallToolResult {
        ensureInitialized()
        val server = toolToServer?.get(name)
        if (server == null) {
            Timber.tag(TAG).e("No server found for tool '$name'")
            return McpCallToolResult(
                content = listOf(McpContent(text = "No MCP server found for tool '$name'")),
                isError = true
            )
        }
        Timber.tag(TAG).d("Routing tool '$name' → ${server.name}")
        return server.client.callTool(name, arguments)
    }

    fun getServerName(toolName: String): String? {
        return toolToServer?.get(toolName)?.name
    }

    companion object {
        private const val TAG = "McpToolClientManager"
    }
}
