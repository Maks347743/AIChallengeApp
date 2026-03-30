# Module: feature/chat

The core chat feature module. Contains the ChatViewModel, all use cases, local tools, and the MCP client integration.

## Package
`com.example.aichallengeapp.feature.chat`

## Key Classes

### ChatViewModel
`presentation/ChatViewModel.kt`

Central coordinator for all chat interactions.

**State:** `ChatState` — messages, inputText, isLoading, error, branches, metrics, taskMemory

**Intents:** `ChatIntent` — SendMessage, UpdateInput, ClearChat, ToggleMetrics, CreateCheckpoint, SwitchBranch, ReconnectMcp

**Message sending strategies:**
- `sendMessageNormal` — standard direct send
- `sendMessageWithSummary` — summarizes older messages for context compression
- `sendMessageWithStickyFacts` — extracts key facts from older messages as context

**`/help` command handling:**
When user input starts with `/help`, ChatViewModel:
1. Strips the prefix, uses the remainder as the actual question
2. Prepends `PromptTemplates.HELP_SYSTEM_PROMPT` to the system prompt
3. Forces AI to use `retrieve` tool and git tools for project-specific answers

**Tool calling loop** (`processToolCallingLoop`):
- Up to `MAX_TOOL_ITERATIONS = 10` iterations
- Each iteration: serialize tool calls → execute → collect results → re-send
- Extracts `__RAG_META__` citations from RAG results

### Use Cases

| Use Case | File | Purpose |
|----------|------|---------|
| `SendChatMessageUseCase` | domain/usecase/ | HTTP POST to AI API |
| `ExecuteToolCallsUseCase` | domain/usecase/ | Dispatch tool calls (local or MCP) |
| `GetToolDefinitionsUseCase` | domain/usecase/ | Aggregate tool schemas from all sources |
| `BuildSystemPromptUseCase` | domain/usecase/ | Compose final system prompt |
| `UpdateTaskMemoryUseCase` | domain/usecase/ | AI-driven task context persistence |
| `UpdateMetricsUseCase` | domain/usecase/ | Token usage tracking |
| `ValidateConstraintsUseCase` | domain/usecase/ | Check response against user constraints |

### System Prompt Construction

`BuildSystemPromptUseCase` assembles:
1. `BASE_SYSTEM_PROMPT` — core AI instructions (tools, retrieve, pipeline usage)
2. `globalPrefix` — user profile description
3. `taskMemory` — AI-maintained cross-session context
4. `chatPrompt` — per-chat custom system prompt
5. Constraints block — if profile has constraints
6. `helpPrefix` — `HELP_SYSTEM_PROMPT` prepended when `/help` command used

### PromptTemplates

`domain/PromptTemplates.kt`

| Constant | Purpose |
|----------|---------|
| `BASE_SYSTEM_PROMPT` | Core assistant instructions (tool use, retrieve, pipeline) |
| `HELP_SYSTEM_PROMPT` | Developer assistant mode (prepended for /help commands) |
| `SUMMARIZER_SYSTEM_PROMPT` | Context summarization |
| `FACTS_EXTRACTOR_SYSTEM_PROMPT` | Sticky facts extraction |

### Local Tools

`data/tools/` — implements `LocalToolHandler` interface

| Tool | Class | Description |
|------|-------|-------------|
| `create_periodic_task` | `CreatePeriodicTaskTool` | Schedule recurring MCP tool + AI summary |
| `stop_periodic_task` | `StopPeriodicTaskTool` | Stop one or all periodic tasks in chat |
| `list_periodic_tasks` | `ListPeriodicTasksTool` | List active periodic tasks |
| `run_pipeline` | `RunPipelineTool` | Multi-step tool chain with variable substitution |

**LocalToolHandler interface:**
```kotlin
interface LocalToolHandler {
    val definition: ToolDefinition
    val isPeriodicTaskTool: Boolean get() = false
    suspend fun execute(arguments: JsonObject?, chatId: String): String
}
```

### MCP Client Integration

`McpToolClientManager` — aggregates multiple MCP servers:
- GitHub MCP Server (port 3001)
- DeepWiki MCP Server
- RAG MCP Server (port 3002)

`McpToolClient` — HTTP client for one MCP server:
- `listTools()` → tool definitions
- `callTool(name, args)` → `McpCallToolResult`

### Chat Repository

`data/repository/ChatRepositoryImpl.kt`

- `sendMessage()` — HTTP POST to `/chat/completions`
- Maps internal roles to API roles
- Handles `ROLE_TOOL_CALL` deserialization (JSON array → `tool_calls` field)
- Handles `ROLE_TOOL_RESULT` → `tool` role with `tool_call_id`

## DI (Koin)

`di/ChatModule.kt` provides:
- `ChatViewModel` factory (scoped by chatId)
- `McpToolClientManager` with all three servers
- All use case factories
- `LocalToolRegistry` — registered in `app/AppModule.kt` with all handlers

## Dependencies

- `core:database` — ChatMessage, ChatSession, repositories
- `core:mcp` — McpTool, ToolDefinition, McpCallToolResult
- `core:periodic-task` — PeriodicTaskMessageBus
- Ktor Client, kotlinx-serialization
