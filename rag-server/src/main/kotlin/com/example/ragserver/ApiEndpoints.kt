package com.example.ragserver

/** External API URLs, model identifiers, and Ollama defaults. */
object ApiEndpoints {
    // DeepSeek
    const val DEEPSEEK_CHAT = "https://api.deepseek.com/v1/chat/completions"
    const val DEEPSEEK_MODEL = "deepseek-chat"

    // Jina AI
    const val JINA_RERANK = "https://api.jina.ai/v1/rerank"
    const val JINA_RERANK_MODEL = "jina-reranker-v2-base-multilingual"

    // DeepWiki
    const val DEEPWIKI_MCP = "https://mcp.deepwiki.com/mcp"

    // Ollama
    const val OLLAMA_DEFAULT_BASE_URL = "http://localhost:11434"
    const val OLLAMA_NATIVE_CHAT_PATH = "/api/chat"
    const val OLLAMA_DEFAULT_CHAT_MODEL = "qwen3:8b"
    const val OLLAMA_DEFAULT_EMBEDDING_MODEL = "nomic-embed-text"
}
