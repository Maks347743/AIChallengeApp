# Module: feature/chat-settings

Settings UI and domain models for per-chat and global app settings.

## Package
`com.example.aichallengeapp.feature.settings`

## Domain Models

### ChatSettings (per-chat)
`domain/model/ChatSettings.kt`

| Field | Default | Description |
|-------|---------|-------------|
| systemPrompt | "" | Custom system prompt injected into AI conversation |
| maxTokens | null | Max response tokens (null = model default) |
| temperature | 1.0 | Sampling temperature |
| summaryEnabled | false | Enable conversation summarization for context compression |
| retainedMessageCount | 10 | Messages to keep before summarizing older ones |
| summaryMaxTokens | 50 | Token limit for summaries |
| slidingWindowEnabled | false | Keep only last N messages in context |
| slidingWindowSize | 10 | Number of messages for sliding window |
| stickyFactsEnabled | false | Extract key facts from older messages as persistent context |
| stickyFactsRecentMessages | 10 | Number of recent messages to keep; extract facts from older |
| ragEnabled | true | Include `retrieve` tool in tool list |

Persisted per chat session in SharedPreferences or Room (keyed by chatId).

### AppSettings (global)
`domain/model/AppSettings.kt`

| Field | Default | Description |
|-------|---------|-------------|
| model | DEEPSEEK_CHAT | Primary AI model (DeepSeekModel enum) |
| ollamaModelName | "qwen3:4b" | Local Ollama model name |
| serverBaseUrl | "" | Home server base URL (routes to MCP servers) |
| mcpServerToken | "" | Auth token for MCP server requests |

### DeepSeekModel (enum)
- `DEEPSEEK_CHAT` → model ID `"deepseek-chat"`, standard API
- `DEEPSEEK_REASONER` → model ID `"deepseek-reasoner"`, with extended thinking
- `LOCAL_MODEL` → uses Ollama with `ollamaModelName`

## Repository

`ChatSettingsRepository` — loads/saves `ChatSettings` per chatId
`AppSettingsRepository` — loads/saves `AppSettings` globally

## UI

`ChatSettingsScreen.kt` — per-chat settings (system prompt, context management, RAG toggle)
`AppSettingsScreen.kt` — global settings (model selection, API keys, server URL)
