# Code Review Rules

This document defines the code review rules used by the AI reviewer.
Each section is a separate RAG chunk retrieved based on the PR diff context.

---

## Android Lifecycle & Memory

- Never store `Context`, `Activity`, or `Fragment` references in a `ViewModel`. Use `applicationContext` if absolutely necessary.
- Always use `viewModelScope` for coroutines in ViewModels — it's automatically cancelled when the ViewModel is cleared.
- Avoid creating coroutine scopes manually (`CoroutineScope(...)`) in ViewModels or Composables. Prefer `viewModelScope` or `rememberCoroutineScope()`.
- Never launch coroutines in `init {}` without proper scope — use `viewModelScope.launch` instead.
- Collect `Flow` in Composables using `collectAsStateWithLifecycle()`, not `collectAsState()`, to respect lifecycle.
- Avoid memory leaks: do not pass lambdas that capture Activity/Fragment into long-lived objects.
- Use `WeakReference` only as a last resort; redesign with proper scoping instead.

---

## Jetpack Compose

- Composable functions should be stateless where possible — hoist state to the nearest common ancestor or ViewModel.
- Avoid creating new lambda instances inside Composables on every recomposition (use `remember { }` or stable references).
- Mark data classes passed to Composables with `@Stable` or `@Immutable` when their fields don't change to avoid unnecessary recompositions.
- Do not perform side effects directly in Composable bodies — use `LaunchedEffect`, `SideEffect`, or `DisposableEffect`.
- Avoid expensive calculations in the Composable body — wrap with `remember(key) { }`.
- Do not use `mutableStateOf` outside of Composable functions or `remember` blocks without a proper scope.
- `LazyColumn` / `LazyRow` items should have stable `key` parameters to optimize recomposition.
- Avoid deeply nested Composables — prefer extracting into separate functions for readability and performance.

---

## Kotlin Coroutines & Flow

- Never use `GlobalScope` — it escapes structured concurrency and can cause leaks.
- Always handle exceptions in coroutines: use `try/catch`, `CoroutineExceptionHandler`, or `catch {}` on Flow.
- Prefer `StateFlow` over `LiveData` for UI state in ViewModels.
- Use `SharedFlow` for one-shot events (navigation, snackbars), not `StateFlow`.
- Avoid `runBlocking` on the main thread — it blocks the UI. Use `launch` or `async` instead.
- Use `flowOn(Dispatchers.IO)` for cold flows doing I/O work; use `withContext(Dispatchers.IO)` in suspend functions.
- Don't use `flow { emit(...) }` for single values — use `suspend fun` instead.
- Cancel coroutines properly when they are no longer needed (structured concurrency handles this via scope).
- When using `collect`, ensure the flow is cancelled on lifecycle end — use `repeatOnLifecycle` or `collectAsStateWithLifecycle`.

---

## Clean Architecture

- Dependencies must point inward: `presentation` → `domain` ← `data`. The `domain` layer must not depend on Android or external frameworks.
- Each `UseCase` should represent a single business operation. Do not combine multiple operations into one use case.
- Use cases should use the `operator fun invoke()` pattern for clean call-site syntax.
- Repository interfaces are defined in the `domain` layer; implementations live in the `data` layer.
- Domain models must not contain Android types (`Context`, `Bundle`, etc.).
- Do not put business logic in ViewModels — delegate to use cases.
- Do not put business logic in Composables or UI components.
- Avoid skipping layers: Composables should not call repositories directly.

---

## Koin DI Patterns

- Use `single { }` for singletons (repositories, services, DAOs) and `factory { }` for use cases and ViewModels.
- ViewModels must be registered with `viewModel { }`, not `single` or `factory`.
- Do not inject dependencies in Composable function parameters — inject in ViewModel or pass as state.
- Keep Koin modules scoped to their feature/layer. Do not define all dependencies in one module.
- Do not call `get()` or `inject()` outside of Koin-managed classes (constructors or module blocks).
- When a module from one feature needs a dependency from another, declare it explicitly — avoid hidden transitive dependencies.
- Always provide test modules that replace real dependencies with fakes for unit testing.

---

## MCP Tool Registration

- Every MCP tool must implement the `Tool` interface with a `register(registry)` method and an `execute(args: Map<String, Any?>)` method.
- Tool names must follow snake_case convention (e.g., `create_periodic_task`).
- Tools must validate their required arguments at the start of `execute()` and return a descriptive error if missing.
- Do not perform long-running blocking operations inside `execute()` — use suspend functions and coroutines.
- Each tool should be registered in the appropriate Koin module and injected into the `LocalToolRegistry`.
- Tool results should be serializable JSON-compatible structures.
- Tool descriptions passed to `register()` must clearly describe the tool's purpose and parameters for the LLM to use it correctly.

---

## Security

- Never hardcode API keys, tokens, or secrets in source code. Use `BuildConfig` fields sourced from `local.properties` (gitignored) or CI secrets.
- Do not log sensitive data (API keys, tokens, user data) via `Log.d/e/i` — these appear in logcat and crash reports.
- Validate all input received from external sources (network, user input, MCP tool arguments) before use.
- Use HTTPS for all network calls. Never allow cleartext traffic in production builds.
- Do not store sensitive data in `SharedPreferences` without encryption — use `EncryptedSharedPreferences` or `DataStore` with encryption.
- Avoid using `Intent` extras for sensitive data passed between activities — prefer in-memory or secure storage.
- When using `WebView`, disable JavaScript unless strictly necessary and validate URLs before loading.

---

## Testing

- ViewModels must be testable without Android framework — use `TestCoroutineDispatcher` / `UnconfinedTestDispatcher` and fake repositories.
- Repository implementations should be tested with a fake DAO or in-memory Room database, not with mocks.
- Use cases must have unit tests covering success, error, and edge-case scenarios.
- Do not test implementation details — test observable behavior (emitted state, returned values).
- Compose UI tests should use `ComposeTestRule` and test user-visible semantics, not internal state.
- Each test should be independent — no shared mutable state between tests.
- Use `turbine` library for testing `Flow` emissions in unit tests.
- Prefer fakes over mocks for repositories and data sources — fakes are more maintainable and closer to real behavior.

---

## Code Quality & Kotlin Idioms

- Prefer `data class` for DTOs and domain models. Avoid mutable `var` fields in data classes.
- Use `sealed class` / `sealed interface` for representing UI state (`Loading`, `Success`, `Error`).
- Avoid nullable types (`T?`) when a non-null value can be guaranteed — use `requireNotNull()` or `checkNotNull()` with a descriptive message.
- Use `when` expressions exhaustively over `if/else` chains for sealed types.
- Prefer extension functions over utility classes for reusable logic scoped to a type.
- Do not suppress warnings with `@Suppress` without a comment explaining why.
- Avoid magic numbers and strings — use named constants or enums.
- Keep functions short and focused on a single responsibility. Functions longer than 40 lines are a signal to refactor.
