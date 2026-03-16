package com.example.ragserver.data

import kotlinx.serialization.Serializable

@Serializable
data class Document(
    val id: String,
    val title: String,
    val source: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
