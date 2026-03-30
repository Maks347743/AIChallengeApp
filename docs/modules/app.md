# Module: app

Main Android application module. Wires all feature and core modules together via Koin DI, hosts foreground services, and defines the app entry point.

## Package
`com.example.aichallengeapp`

## Entry Points

### MainApplication
`MainApplication.kt`

- Initializes Koin with all modules
- Resumes active periodic tasks on startup: `PeriodicTaskManager.resumeIfNeeded()`

### MainActivity
`MainActivity.kt`

- Sets up Compose navigation
- Root nav graph: ChatList → Chat, Settings, UserPreferences

## DI (AppModule)

`di/AppModule.kt` — main Koin module loaded by MainApplication.

**Key bindings:**
- `LocalToolRegistry` — with all four local tool handlers:
  ```kotlin
  LocalToolRegistry(listOf(
      CreatePeriodicTaskTool(get(), get()),
      StopPeriodicTaskTool(get(), get()),
      ListPeriodicTasksTool(get()),
      RunPipelineTool(get(), get(), get())
  ))
  ```
- `PeriodicTaskExecutor` — executes MCP tools + AI summarization for periodic tasks
- `PeriodicTaskServiceController` — Android service controller
- `HomeServerConfig` — configures MCP server base URL

## Services

### PeriodicTaskService
`service/PeriodicTaskService.kt`

Android Foreground Service that maintains coroutine timers for active periodic tasks.

- Started/stopped by `PeriodicTaskManager` via `PeriodicTaskServiceController`
- On start: loads all active tasks from DB, starts a coroutine timer per task
- Each timer: calls `PeriodicTaskExecutor.execute(task)` at `intervalMinutes` intervals

### PeriodicTaskExecutor
`service/PeriodicTaskExecutor.kt`

Executes one periodic task run:
1. `McpToolClientManager.callTool(toolName, toolArgs)`
2. AI summarization of the result (DeepSeek or local model)
3. `PeriodicTaskMessageBus.emit()` — updates the associated chat
4. Updates `lastExecutedAt` in DB

## Explicit Dependencies

The `app` module must explicitly declare some transitive dependencies because `feature/chat` uses `implementation` (not `api`) for them:

- `core:mcp` — needed for `McpTool`, `ToolDefinition` models
- `kotlinx-serialization-json` — needed for JSON operations in AppModule

## Navigation

```
NavHost
  ├── chatList (ChatListScreen)
  │     └── → chat/{chatId}
  ├── chat/{chatId} (ChatScreen)
  ├── settings/{chatId} (ChatSettingsScreen)
  └── userPreferences (UserPreferencesScreen)
```
