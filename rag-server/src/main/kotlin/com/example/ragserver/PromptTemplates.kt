package com.example.ragserver

/** All LLM system prompts and user prompt builders used across the RAG server. */
object PromptTemplates {

    const val QUERY_REWRITE_SYSTEM =
        "Rewrite the user's search query to be more detailed and semantically rich for " +
        "embedding-based document retrieval. Output only the rewritten query, nothing else."

    const val RERANK_SYSTEM =
        "You are a document relevance ranker. Given a search query and numbered documents, " +
        "output the document numbers ranked from most to least relevant, comma-separated. " +
        "Output ONLY the numbers."

    fun rerankUser(query: String, numberedDocs: String, n: Int): String =
        "Query: $query\n\nDocuments:\n$numberedDocs\n\n" +
        "Output the document numbers ranked from most to least relevant to the query, " +
        "comma-separated. Include ALL $n numbers. Output ONLY the numbers, nothing else. " +
        "Example: 3,1,4,2"
}
