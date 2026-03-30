# Module: core/mcp

Shared MCP (Model Context Protocol) protocol models used by both the Android app and the servers.

## Package
`com.example.aichallengeapp.core.mcp`

## Models

### McpTool
```kotlin
data class McpTool(
    val name: String,
    val description: String,
    val inputSchema: JsonElement
)
```
Represents a tool definition as returned by `tools/list`.

### McpCallToolResult
```kotlin
data class McpCallToolResult(
    val content: List<McpContent>,
    val isError: Boolean? = null
)
```

### McpContent
```kotlin
data class McpContent(
    val type: String = "text",
    val text: String? = null
)
```

### ToolDefinition
OpenAI-compatible tool definition used in chat completions requests:
```kotlin
data class ToolDefinition(
    val type: String = "function",
    val function: FunctionDefinition
)

data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: JsonElement
)
```

### ToolCallInfo
Parsed tool call from an AI response:
```kotlin
data class ToolCallInfo(
    val id: String,
    val name: String,
    val arguments: JsonObject?
)
```

### JsonRpcRequest / JsonRpcResponse
MCP JSON-RPC 2.0 envelope models used by all MCP servers.

### HomeServerConfig
```kotlin
data class HomeServerConfig(val baseUrl: String)
```
Configured in `AppModule`, points to the home/proxy server that routes to MCP servers.

## Constants (McpProtocol)

- `METHOD_INITIALIZE` — `"initialize"`
- `METHOD_TOOLS_LIST` — `"tools/list"`
- `METHOD_TOOLS_CALL` — `"tools/call"`
- `PROTOCOL_VERSION` — `"2024-11-05"`
