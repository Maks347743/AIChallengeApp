# Chat API

The Android app communicates with AI models via OpenAI-compatible chat completions API.

## Endpoint

```
POST {baseUrl}/chat/completions
Authorization: Bearer {apiKey}
Content-Type: application/json
```

**Default endpoint:** DeepSeek API (`https://api.deepseek.com`)
**Local model:** Ollama via configured `serverBaseUrl`

## Request

```json
{
  "model": "deepseek-chat",
  "messages": [
    {"role": "system", "content": "System prompt..."},
    {"role": "user", "content": "User message"},
    {"role": "assistant", "content": "Previous AI response"},
    {
      "role": "assistant",
      "tool_calls": [
        {
          "id": "call_abc123",
          "type": "function",
          "function": {"name": "retrieve", "arguments": "{\"query\":\"RAG architecture\"}"}
        }
      ]
    },
    {
      "role": "tool",
      "tool_call_id": "call_abc123",
      "content": "Tool result text..."
    }
  ],
  "max_tokens": 2048,
  "temperature": 1.0,
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "retrieve",
        "description": "Search indexed documentation using semantic similarity",
        "parameters": {
          "type": "object",
          "properties": {
            "query": {"type": "string", "description": "Search query in English"}
          },
          "required": ["query"]
        }
      }
    }
  ]
}
```

## Response

```json
{
  "id": "chatcmpl-abc",
  "choices": [
    {
      "message": {
        "role": "assistant",
        "content": "Response text or null if tool_calls",
        "tool_calls": [
          {
            "id": "call_xyz",
            "type": "function",
            "function": {"name": "retrieve", "arguments": "{\"query\":\"search term\"}"}
          }
        ]
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 150,
    "completion_tokens": 80,
    "total_tokens": 230
  }
}
```

**finish_reason values:**
- `stop` — normal completion
- `tool_calls` — AI wants to call a tool (triggers tool calling loop)
- `length` — hit max_tokens limit

## Role Mapping

Internal `ChatMessage` roles are mapped before sending:

| Internal Role | Sent As |
|---------------|---------|
| `user` | `user` |
| `assistant` | `assistant` |
| `system` | `system` |
| `tool_call` | `assistant` with `tool_calls` array (deserialized from JSON) |
| `tool_result` | `tool` with `tool_call_id` |
| `summary` | `user` (prefixed as context) |
| `facts` | `user` (sticky facts) |
| `constraint_violation_assistant` | `assistant` |
| `constraint_violation_user` | `user` |

## Token Costs (DeepSeek)

- Input: $0.28 / 1M tokens
- Output: $0.42 / 1M tokens

Tracked per session in `ChatMetricsEntity`.

## Models

| ID | Name | Use case |
|----|------|----------|
| `deepseek-chat` | DeepSeek Chat | Default, best quality |
| `deepseek-reasoner` | DeepSeek Reasoner | Complex reasoning tasks |
| Ollama model name | Local | Offline / privacy |

Configured in `AppSettings.model` and `AppSettings.ollamaModelName`.
