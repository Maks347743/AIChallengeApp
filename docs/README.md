# AIChallengeApp

AI-powered chat application for Android that integrates Claude/DeepSeek language models with an extensible MCP (Model Context Protocol) tool ecosystem, RAG (Retrieval-Augmented Generation), and a local developer assistant.

## Features

- **AI Chat** — Conversations with DeepSeek or local Ollama models
- **MCP Tools** — Extensible tool system: GitHub search, RAG retrieval, git inspection
- **RAG** — Semantic search over indexed project documentation
- **Periodic Tasks** — Schedule recurring tool executions with AI summaries
- **Pipeline Execution** — Chain multiple tool calls into automated workflows
- **Developer Assistant** — `/help` command for project-aware Q&A with docs + git context

## Module Structure

| Module | Description |
|--------|-------------|
| `app/` | Main Android app — DI wiring, services, Koin modules |
| `core/database` | Room DB, entities, DAOs, repositories, domain models |
| `core/mcp` | Shared MCP protocol models (JsonRpc, McpTool, ToolDefinition) |
| `core/periodic-task` | Periodic task domain logic and service controller |
| `feature/chat` | Chat UI, ChatViewModel, use cases, local tools, MCP client |
| `feature/chat-list` | Chat session list screen |
| `feature/chat-settings` | Per-chat and global settings UI + domain models |
| `feature/user-preferences` | User profile management |
| `github-mcp-server/` | Standalone Ktor MCP server (port 3001) — GitHub + git tools |
| `rag-server/` | Standalone Compose Desktop RAG server (port 3002) |

## Quick Start

### Android App
```bash
./gradlew app:installDebug
```

### GitHub MCP Server (with git tools)
```bash
cd github-mcp-server
./gradlew run --args="--project-dir /path/to/your/project"
# GITHUB_TOKEN env variable optional for higher rate limits
```

### RAG Server
```bash
cd rag-server
# Start with UI (for document management and indexing):
./gradlew run

# Start headless (server only):
./gradlew run --args="--headless"

# Import project docs into RAG index:
./gradlew run --args="--import /path/to/docs"
```

### Index project docs into RAG
```bash
cd rag-server
./gradlew run --args="--import /path/to/AIChallengeApp/docs"
```

## Architecture Overview

```
Android App
  └── ChatViewModel
        ├── Local Tools (create_periodic_task, stop_periodic_task, list_periodic_tasks, run_pipeline)
        └── McpToolClientManager
              ├── GitHub MCP Server :3001  (github_search_repos, get_git_branch, get_git_diff, ...)
              └── RAG MCP Server    :3002  (retrieve)
```

### Tool Calling Flow
1. User sends message → ChatViewModel builds system prompt + tool definitions
2. AI responds with `tool_calls` → `ExecuteToolCallsUseCase` dispatches to local or MCP tools
3. Tool results returned to AI → AI generates final response
4. RAG citations extracted and appended to response

### `/help` Command
Type `/help <question>` in chat to activate the developer assistant mode. The assistant will:
- Always call `retrieve` first to search project documentation
- Use `get_git_branch` / `get_git_diff` for current code state queries
- Answer specifically about this project's architecture and modules

## Documentation

- [Architecture & Data Flow](overview.md)
- [Data Schemas](data-schemas.md)
- [MCP Tools API](api/mcp-tools.md)
- [Chat API](api/chat-api.md)
- **Modules:**
  - [app](modules/app.md)
  - [core/database](modules/core-database.md)
  - [core/mcp](modules/core-mcp.md)
  - [feature/chat](modules/feature-chat.md)
  - [feature/chat-settings](modules/feature-settings.md)
  - [github-mcp-server](modules/github-mcp-server.md)
  - [rag-server](modules/rag-server.md)

## Tech Stack

- **Android**: Kotlin, Jetpack Compose, Room, Koin, Ktor Client
- **Servers**: Kotlin, Ktor Server, Compose Desktop (RAG UI)
- **AI Models**: DeepSeek API, Ollama (local), Jina (reranking)
- **Protocol**: MCP (Model Context Protocol) over HTTP JSON-RPC
