package com.example.ragserver.config

import kotlinx.serialization.Serializable

@Serializable
data class RagConfig(
    val useQueryRewrite: Boolean = false,
    val deepSeekApiKey: String = "",
    val useRerank: Boolean = false,
    val jinaApiKey: String = "",
    val topK: Int = 3,
    val initialK: Int = 20,
    val similarityThreshold: Float = 0.3f
)
