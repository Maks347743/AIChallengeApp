package com.example.ragserver.embedding

interface EmbeddingService {
    suspend fun embed(text: String): FloatArray?
}
