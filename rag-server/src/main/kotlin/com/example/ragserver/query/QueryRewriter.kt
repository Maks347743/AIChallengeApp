package com.example.ragserver.query

interface QueryRewriter {
    suspend fun rewrite(query: String): String
}
