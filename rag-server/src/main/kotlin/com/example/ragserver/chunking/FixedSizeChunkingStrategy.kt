package com.example.ragserver.chunking

import com.example.ragserver.data.Chunk
import com.example.ragserver.data.ChunkMetadata
import com.example.ragserver.data.Document
import java.util.UUID

class FixedSizeChunkingStrategy(
    private val windowSize: Int = 500,
    overlap: Int = 50
) : ChunkingStrategy {
    private val stepSize = windowSize - overlap

    override fun chunk(doc: Document): List<Chunk> {
        val words = doc.content.split(Regex("\\s+")).filter { it.isNotBlank() }
        val chunks = mutableListOf<Chunk>()
        var index = 0
        var chunkIdx = 0

        while (index < words.size) {
            val end = minOf(index + windowSize, words.size)
            val chunkWords = words.subList(index, end)

            // Skip trailing chunks smaller than windowSize,
            // unless this is the only chunk for the document
            if (chunkWords.size < windowSize && chunks.isNotEmpty()) break

            chunks.add(
                Chunk(
                    id = UUID.randomUUID().toString(),
                    docId = doc.id,
                    text = chunkWords.joinToString(" "),
                    metadata = ChunkMetadata(
                        title = doc.title,
                        file = doc.source,
                        section = null,
                        chunkIndex = chunkIdx++,
                        strategy = "fixed"
                    )
                )
            )
            index += stepSize
        }
        return chunks
    }
}
