# Data Schemas

## Android Room Database (core/database)

DB version: 10. Located at `core/database/`.

### ChatSessionEntity

Table: `chat_sessions`

| Field | Type | Description |
|-------|------|-------------|
| id | String (PK) | UUID |
| title | String | Display title |
| createdAt | Long | Unix timestamp ms |
| profileId | String? | Associated user profile |
| parentId | String? | Parent session (for branches) |
| branchIndex | Int | 0 = main, 1+ = branches |
| isPeriodicTask | Boolean | True if used for periodic task output |
| taskMemory | String? | AI-maintained task context across sessions |

### ChatMessageEntity

Table: `chat_messages`

| Field | Type | Description |
|-------|------|-------------|
| id | String (PK) | UUID |
| chatId | String (FK) | References chat_sessions.id |
| role | String | See roles below |
| content | String | Message text |
| timestamp | Long | Unix timestamp ms |

**Roles:**
- `user` — user message
- `assistant` — AI response
- `system` — system prompt (not persisted normally)
- `tool_call` — AI tool invocation (serialized JSON array)
- `tool_result` — tool execution result
- `summary` — conversation summary (for context compression)
- `facts` — extracted sticky facts
- `constraint_violation_assistant` — AI response that violated constraints
- `constraint_violation_user` — correction request after violation

### UserProfileEntity

Table: `user_profiles`

| Field | Type | Description |
|-------|------|-------------|
| id | String (PK) | UUID |
| name | String | Display name |
| description | String | Injected as global system prompt prefix |
| constraints | String | JSON array of Constraint objects |

**Constraint (domain model):**
```kotlin
data class Constraint(val name: String, val description: String)
```

### PeriodicTaskEntity

Table: `periodic_tasks`

| Field | Type | Description |
|-------|------|-------------|
| id | String (PK) | UUID |
| chatId | String | Associated chat session |
| toolName | String | MCP tool to execute |
| toolArgumentsJson | String | JSON string of tool args |
| intervalMinutes | Int | Execution interval |
| prompt | String | Summarization prompt |
| isActive | Boolean | Whether task is running |
| lastExecutedAt | Long? | Last execution timestamp |

### PeriodicTaskResultEntity

Table: `periodic_task_results`

| Field | Type | Description |
|-------|------|-------------|
| id | String (PK) | UUID |
| taskId | String (FK) | References periodic_tasks.id |
| result | String | Tool output |
| summary | String | AI summary |
| executedAt | Long | Execution timestamp |

### ChatMetricsEntity

Table: `chat_metrics`

| Field | Type | Description |
|-------|------|-------------|
| chatId | String (PK) | Chat session ID |
| totalInputTokens | Long | Cumulative input tokens |
| totalOutputTokens | Long | Cumulative output tokens |
| totalCostUsd | Double | Estimated cost |
| messageCount | Int | Total messages |

## Domain Models (core/database)

### ChatMessage
```kotlin
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
```

### ChatSession
```kotlin
data class ChatSession(
    val id: String,
    val title: String,
    val messages: List<ChatMessage>,
    val profileId: String?,
    val parentId: String?,
    val branchIndex: Int,
    val isPeriodicTask: Boolean,
    val taskMemory: String?
)
```

### ChatResult
```kotlin
data class ChatResult(
    val message: String,
    val toolCalls: List<ToolCallInfo>?,
    val finishReason: String?,
    val metrics: ChatMetrics?
)
```

## RAG Server Data Models (rag-server)

### Document
```kotlin
data class Document(
    val id: String,       // UUID
    val title: String,
    val source: String,   // file path or "deepwiki:owner/repo/type"
    val content: String,
    val createdAt: Long
)
```
Stored as: `~/.ragserver/docs/{id}.json`

### Chunk
```kotlin
data class Chunk(
    val id: String,           // UUID
    val docId: String,        // Parent document ID
    val text: String,         // Chunk text content
    val metadata: ChunkMetadata
)

data class ChunkMetadata(
    val title: String,         // Document title
    val file: String,          // Source file path
    val section: String?,      // Markdown section header
    val chunkIndex: Int,
    val strategy: String       // "structural" or "fixed"
)
```
Stored as: `~/.ragserver/chunks/{id}.json`

### VectorIndex
- Stored as: `~/.ragserver/index/index.bin`
- Format: JSON map of `chunkId → List<Float>` (embedding vector)
- In-memory: `ConcurrentHashMap<String, FloatArray>`

## Settings Models (feature/chat-settings)

### ChatSettings (per-chat)
```kotlin
data class ChatSettings(
    val systemPrompt: String = "",
    val maxTokens: Int? = null,
    val temperature: Float = 1.0f,
    val summaryEnabled: Boolean = false,
    val retainedMessageCount: Int = 10,
    val summaryMaxTokens: Int = 50,
    val slidingWindowEnabled: Boolean = false,
    val slidingWindowSize: Int = 10,
    val stickyFactsEnabled: Boolean = false,
    val stickyFactsRecentMessages: Int = 10,
    val ragEnabled: Boolean = true
)
```

### AppSettings (global)
```kotlin
data class AppSettings(
    val model: DeepSeekModel = DEEPSEEK_CHAT,
    val ollamaModelName: String = "qwen3:4b",
    val serverBaseUrl: String = "",
    val mcpServerToken: String = ""
)
```

### RagConfig (rag-server)
```kotlin
data class RagConfig(
    val useQueryRewrite: Boolean = false,
    val deepSeekApiKey: String = "",
    val useRerank: Boolean = false,
    val jinaApiKey: String = "",
    val topK: Int = 3,
    val initialK: Int = 20,
    val similarityThreshold: Float = 0.3f,
    val useLocalModel: Boolean = false,
    val ollamaBaseUrl: String = "http://localhost:11434",
    val ollamaChatModel: String = "qwen3:8b",
    val ollamaEmbeddingModel: String = "nomic-embed-text"
)
```
Stored as: `~/.ragserver/config.json`
