package com.example.ragserver.chunking

import com.example.ragserver.data.Chunk
import com.example.ragserver.data.Document

interface ChunkingStrategy {
    fun chunk(doc: Document): List<Chunk>
}
