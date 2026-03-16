package com.example.ragserver.data

import kotlinx.serialization.Serializable

@Serializable
data class ChunkMetadata(
    val title: String,
    val file: String,
    val section: String? = null,
    val chunkIndex: Int,
    val strategy: String
)
