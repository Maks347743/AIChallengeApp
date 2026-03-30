# Module: core/database

Room database module. Contains all entities, DAOs, repositories, and domain models shared across features.

## Package
`com.example.aichallengeapp.core.database`

## Database

`AppDatabase.kt` — Room database, version 10.

Migrations: `1→2`, `2→3`, ... `9→10` (periodic_tasks + periodic_task_results tables added in 9→10).

## Entities & DAOs

| Entity | Table | DAO |
|--------|-------|-----|
| `ChatSessionEntity` | `chat_sessions` | `ChatSessionDao` |
| `ChatMessageEntity` | `chat_messages` | `ChatMessageDao` |
| `UserProfileEntity` | `user_profiles` | `UserProfileDao` |
| `PeriodicTaskEntity` | `periodic_tasks` | `PeriodicTaskDao` |
| `PeriodicTaskResultEntity` | `periodic_task_results` | `PeriodicTaskResultDao` |
| `ChatMetricsEntity` | `chat_metrics` | `ChatMetricsDao` |

## Domain Models

`domain/model/` — data classes used across features (not Room entities).

- `ChatMessage` — role + content + timestamp
- `ChatSession` — id, title, messages, profileId, branches, taskMemory
- `ChatResult` — AI response with message, toolCalls, finishReason, metrics
- `Constraint` — name + description (serialized to JSON in UserProfileEntity)
- `UserProfile` — id, name, description, constraints
- `PeriodicTask` — id, chatId, toolName, toolArgumentsJson, intervalMinutes, prompt, isActive, lastExecutedAt
- `ChatMetrics` — token counts and cost

## Repositories

`domain/repository/` — interfaces
`data/repository/` — Room implementations

| Interface | Implementation | Purpose |
|-----------|---------------|---------|
| `ChatRepository` | `ChatRepositoryImpl` | Send messages to AI API, persist sessions |
| `ChatSessionRepository` | `ChatSessionRepositoryImpl` | CRUD for chat sessions |
| `UserProfileRepository` | `UserProfileRepositoryImpl` | CRUD for user profiles |
| `PeriodicTaskRepository` | `PeriodicTaskRepositoryImpl` | CRUD for periodic tasks |
| `ChatMetricsRepository` | `ChatMetricsRepositoryImpl` | Token usage tracking |

## DI (Koin)

`di/DatabaseModule.kt` provides:
- `AppDatabase` singleton
- All DAOs as singletons
- All repository implementations as singletons

## Shared State

`domain/model/PeriodicTaskMessageBus.kt`

```kotlin
object PeriodicTaskMessageBus {
    val messages: SharedFlow<PeriodicTaskMessage>
    fun emit(message: PeriodicTaskMessage)
}
```

Singleton shared between `app` module (PeriodicTaskService) and `feature/chat` (ChatViewModel). Allows periodic task results to update the chat UI without direct coupling.
