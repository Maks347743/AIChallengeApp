package com.example.ragserver.reranking

interface Reranker {
    /**
     * Returns relevance scores in the same order as [documents].
     * Returns emptyList() on failure (fail-safe).
     */
    suspend fun rerank(query: String, documents: List<String>): List<Float>
}
