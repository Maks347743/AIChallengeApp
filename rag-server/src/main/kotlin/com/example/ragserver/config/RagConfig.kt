package com.example.ragserver.config

import com.example.ragserver.ApiEndpoints
import kotlinx.serialization.Serializable

@Serializable
data class RagConfig(
    val useQueryRewrite: Boolean = false,
    val deepSeekApiKey: String = "",
    val useRerank: Boolean = false,
    val jinaApiKey: String = "",
    val topK: Int = DEFAULT_TOP_K,
    val initialK: Int = DEFAULT_INITIAL_K,
    val similarityThreshold: Float = DEFAULT_SIMILARITY_THRESHOLD,
    val useLocalModel: Boolean = false,
    val ollamaBaseUrl: String = ApiEndpoints.OLLAMA_DEFAULT_BASE_URL,
    val ollamaChatModel: String = ApiEndpoints.OLLAMA_DEFAULT_CHAT_MODEL,
    val ollamaEmbeddingModel: String = ApiEndpoints.OLLAMA_DEFAULT_EMBEDDING_MODEL,
    val serverToken: String? = null
) {
    companion object {
        const val DEFAULT_TOP_K = 3
        const val DEFAULT_INITIAL_K = 20
        const val DEFAULT_SIMILARITY_THRESHOLD = 0.3f
    }
}
