package com.example.ragserver.data

import kotlinx.serialization.Serializable

@Serializable
data class Chunk(
    val id: String,
    val docId: String,
    val text: String,
    val metadata: ChunkMetadata
)
