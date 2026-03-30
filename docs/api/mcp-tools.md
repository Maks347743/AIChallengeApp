# MCP Tools API

All tools use the MCP (Model Context Protocol) over HTTP JSON-RPC.

**Protocol:** `POST /mcp`
**Format:** JSON-RPC 2.0

## Standard MCP Methods

### initialize
Returns server capabilities.

**Request:**
```json
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{}}}
```

**Response:**
```json
{"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2024-11-05","capabilities":{"tools":{}},"serverInfo":{"name":"GitHub MCP Server","version":"1.0.0"}}}
```

### tools/list
Returns available tools.

**Request:**
```json
{"jsonrpc":"2.0","id":2,"method":"tools/list"}
```

### tools/call
Executes a tool.

**Request:**
```json
{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"tool_name","arguments":{}}}
```

---

## GitHub MCP Server (port 3001)

Started with: `./gradlew run --args="--project-dir /path/to/project"`

### github_search_repos

Search GitHub repositories.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| query | string | yes | Search query |
| maxResults | integer | no | Max results (default: 5) |

**Response:** List of repos with name, description, stars, language, URL.

---

### github_get_user

Fetch GitHub user information.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| username | string | yes | GitHub username |

---

### github_trending

Get trending GitHub repositories.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| language | string | no | Filter by programming language |
| period | string | no | `daily`, `weekly`, `monthly`, `yearly`, `all_time` |
| maxResults | integer | no | Max results (default: 5) |

---

### get_git_branch

Returns the current git branch of the project.

**Parameters:** None

**Response:**
```
Current branch: main
```

---

### get_git_diff

Returns the current git diff of the project.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| staged_only | boolean | no | If true, show only staged changes (default: false) |

**Response:** Unified diff text, truncated to 8000 characters if larger.

---

## RAG MCP Server (port 3002)

Started with: `./gradlew run --headless` or `./gradlew run` (with UI)

Import docs: `./gradlew run --args="--import /path/to/docs"`

### retrieve

Semantic search over indexed documentation.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| query | string | yes | Search query (use English for best results) |
| maxResults | integer | no | Number of results (default: from config topK=3) |

**Response:**
```
[Result 1]
Source: /path/to/file.md
Section: Module Structure
---
<chunk text>

[Result 2]
...
```

Also includes a metadata content block prefixed with `__RAG_META__:` containing JSON chunk metadata for citation tracking (internal use by `ExecuteToolCallsUseCase`).

**Notes:**
- Query should always be in English for best embedding match
- Similarity threshold filtering applies (default: 0.3)
- Optional query rewriting and reranking per `RagConfig`

---

## Local Tools (Android app, feature/chat)

These tools are executed directly in the Android app process, not via MCP.

### create_periodic_task

Schedule a recurring MCP tool execution.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| tool_name | string | yes | MCP tool to execute periodically |
| tool_arguments | string | no | JSON string of tool arguments |
| interval_minutes | integer | yes | Minutes between executions |
| prompt | string | yes | Summarization prompt for AI |

### stop_periodic_task

Stop a periodic task.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| task_id | string | no | Task ID to stop; if omitted, stops all tasks in current chat |

### list_periodic_tasks

List active periodic tasks for the current chat.

**Parameters:** None

### run_pipeline

Execute a multi-step tool pipeline with variable substitution.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| steps | array | yes | Array of pipeline steps (max 10) |
| summary_prompt | string | yes | Final summarization instruction |

**Step types:**

`tool_call` step:
```json
{"type": "tool_call", "tool": "github_search_repos", "args": {"query": "{{search_term}}"}}
```

`extract` step:
```json
{"type": "extract", "prompt": "Extract the repository name", "output_var": "repo_name"}
```

Variables from `extract` steps are available as `{{variable_name}}` in subsequent steps.
