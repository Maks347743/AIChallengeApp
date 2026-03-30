# Module: github-mcp-server

Standalone Ktor server providing GitHub API tools and local git inspection tools via MCP.

## Package
`com.example.githubmcpserver`

## Port
3001

## Startup

```bash
cd github-mcp-server
./gradlew run --args="--project-dir /path/to/your/project"
# Optional: export GITHUB_TOKEN=your_token (for higher rate limits)
```

`--project-dir` sets the working directory for git commands (defaults to `user.dir`).

## Architecture

```
Main.kt
  ├── Parse --project-dir argument
  ├── GitHubApiClient (GitHub REST API HTTP client)
  └── GitHubToolRegistry
        ├── GitHubSearchReposTool
        ├── GitHubGetUserTool
        ├── GitHubTrendingTool
        ├── GitBranchTool (projectDir)
        └── GitDiffTool (projectDir)
```

## MCP Route

`routes/GitHubMcpRoutes.kt` — `POST /mcp`

Handled by `GitHubMcpRequestHandler`:
- `initialize` → server info + capabilities
- `tools/list` → all registered tool schemas
- `tools/call` → dispatch to `GitHubToolRegistry.callTool()`

## Tool Interface

```kotlin
interface GitHubMcpToolHandler {
    val name: String
    val description: String
    val inputSchema: JsonElement  // JSON Schema object
    suspend fun execute(arguments: JsonObject?): McpCallToolResult
}
```

## Available Tools

### github_search_repos
`tools/GitHubSearchReposTool.kt`

Searches GitHub repositories via `GET /search/repositories?q=...`.

Parameters: `query` (required), `maxResults` (default: 5)

Returns: name, description, stars, language, URL for each repo.

### github_get_user
`tools/GitHubGetUserTool.kt`

Fetches GitHub user profile via `GET /users/{username}`.

Parameters: `username` (required)

### github_trending
`tools/GitHubTrendingTool.kt`

Gets trending repositories by filtering recent repos by star count.

Parameters: `language`, `period` (daily/weekly/monthly/yearly/all_time), `maxResults`

### get_git_branch
`tools/GitBranchTool.kt`

Returns the current git branch name.

Runs: `git -C {projectDir} branch --show-current`

Parameters: None

### get_git_diff
`tools/GitDiffTool.kt`

Returns the current git diff (staged + unstaged, or staged only).

Runs: `git -C {projectDir} diff HEAD` or `git diff --cached` if `staged_only=true`

Truncated to 8000 characters.

Parameters: `staged_only` (boolean, default: false)

## GitHub API Client

`github/GitHubApiClient.kt`

- Base URL: `https://api.github.com`
- Auth: `Authorization: Bearer {GITHUB_TOKEN}` if token set
- Handles rate limit errors (`GitHubAuthException`)

## Adding New Tools

1. Create class implementing `GitHubMcpToolHandler` in `tools/`
2. Register in `Main.kt` in the `GitHubToolRegistry` constructor list

No changes to the registry or request handler needed — the registry uses the list directly.
