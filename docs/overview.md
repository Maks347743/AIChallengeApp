# Architecture & Data Flow

## System Overview

AIChallengeApp is a multi-module Android application with two companion server processes. The Android app acts as an AI chat client, while external MCP servers provide tool capabilities. All communication uses the MCP (Model Context Protocol) over HTTP JSON-RPC.

```
┌─────────────────────────────────────────────────┐
│              Android App                         │
│                                                  │
│  ChatScreen → ChatViewModel                      │
│       │                                          │
│       ├── BuildSystemPromptUseCase               │
│       ├── SendChatMessageUseCase ──► DeepSeek API│
│       ├── ExecuteToolCallsUseCase                │
│       │       ├── LocalToolRegistry              │
│       │       │   ├── create_periodic_task       │
│       │       │   ├── stop_periodic_task         │
│       │       │   ├── list_periodic_tasks        │
│       │       │   └── run_pipeline               │
│       │       └── McpToolClientManager           │
│       │           ├──► GitHub MCP :3001          │
│       │           └──► RAG MCP    :3002          │
│       └── PeriodicTaskService (foreground)       │
└─────────────────────────────────────────────────┘

┌─────────────────────┐   ┌─────────────────────────┐
│  GitHub MCP :3001   │   │  RAG Server :3002        │
│                     │   │                          │
│ github_search_repos │   │ retrieve                 │
│ github_get_user     │   │  └── RetrievalPipeline   │
│ github_trending     │   │       ├── QueryRewriter  │
│ get_git_branch      │   │       ├── VectorIndex    │
│ get_git_diff        │   │       ├── ChunkStorage   │
└─────────────────────┘   │       └── Reranker       │
                          └─────────────────────────┘
```

## Message Processing Flow

```
User Input
    │
    ├── /help detected? → prepend HELP_SYSTEM_PROMPT to system prompt
    │
    ▼
ChatViewModel.sendMessage()
    │
    ├── Load ChatSettings, AppSettings, UserProfile
    ├── Build system prompt (BASE + profile + constraints + taskMemory + helpPrefix)
    ├── Select strategy:
    │   ├── stickyFacts: extract facts from older messages, use as context
    │   ├── summary: summarize older messages, include summary in prompt
    │   └── normal: direct message send
    │
    ▼
SendChatMessageUseCase → HTTP POST /chat/completions
    │
    ▼
ChatResponse
    │
    ├── finishReason = "tool_calls"?
    │   └── processToolCallingLoop (up to 10 iterations):
    │       ├── ExecuteToolCallsUseCase (local or MCP)
    │       ├── Collect ROLE_TOOL_RESULT messages
    │       ├── Extract __RAG_META__ chunk citations
    │       └── Re-send with tool results → repeat
    │
    ├── ValidateConstraintsUseCase (retry up to 3x if violations)
    ├── splitCitations() at "---ЦИТАТЫ---" marker
    ├── appendSourcesIfNeeded() with chunk metadata
    ├── UpdateTaskMemoryUseCase
    └── UpdateMetricsUseCase
```

## RAG Retrieval Pipeline

```
User Query
    │
    ├── QueryRewriter (optional): reformulate for better embedding match
    │
    ▼
OllamaEmbeddingService → FloatArray embedding
    │
    ▼
VectorIndex.searchWithScores() → cosine similarity → top-initialK chunks
    │
    ├── Reranker (optional): cross-encoder reranking → top-topK chunks
    ├── Filter by similarityThreshold
    │
    ▼
ChunkStorage.load() → Chunk text + metadata
    │
    ▼
RetrieveTool response → AI context + __RAG_META__ citation data
```

## Periodic Task Execution

```
create_periodic_task tool called
    │
    ▼
PeriodicTask saved to Room DB
PeriodicTaskManager.onTaskCreated() → PeriodicTaskService.ensureStarted()
    │
    ▼
PeriodicTaskService (Foreground Service)
    └── coroutine timer per task
        └── every intervalMinutes:
            ├── McpToolClientManager.callTool(toolName, toolArgs)
            ├── DeepSeek summarization of result
            └── PeriodicTaskMessageBus.emit() → ChatViewModel updates chat
```

## Module Dependencies

```
app
 ├── feature/chat
 │    ├── core/database
 │    ├── core/mcp
 │    └── core/periodic-task
 ├── feature/chat-list
 ├── feature/chat-settings
 └── feature/user-preferences
```
