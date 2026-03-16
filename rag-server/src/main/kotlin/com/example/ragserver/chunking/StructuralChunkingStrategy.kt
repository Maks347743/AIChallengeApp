package com.example.ragserver.chunking

import com.example.ragserver.data.Chunk
import com.example.ragserver.data.ChunkMetadata
import com.example.ragserver.data.Document
import java.util.UUID

class StructuralChunkingStrategy(
    private val maxWordsPerChunk: Int = 300
) : ChunkingStrategy {

    override fun chunk(doc: Document): List<Chunk> {
        val lines = doc.content.lines()
        val chunks = mutableListOf<Chunk>()
        val headerRegex = Regex("^(#{1,6}\\s+.+|---.*)$")

        var currentSection: String? = null
        val currentLines = mutableListOf<String>()
        var chunkIdx = 0

        fun flush() {
            val text = currentLines.joinToString("\n").trim()
            if (text.isBlank()) return

            val words = text.split(Regex("\\s+"))
            if (words.size <= maxWordsPerChunk) {
                chunks.add(Chunk(
                    id = UUID.randomUUID().toString(),
                    docId = doc.id,
                    text = text,
                    metadata = ChunkMetadata(
                        title = doc.title,
                        file = doc.source,
                        section = currentSection,
                        chunkIndex = chunkIdx++,
                        strategy = "structural"
                    )
                ))
            } else {
                // Section too large — split into sub-chunks of maxWordsPerChunk words
                var i = 0
                while (i < words.size) {
                    val slice = words.subList(i, minOf(i + maxWordsPerChunk, words.size)).joinToString(" ")
                    chunks.add(Chunk(
                        id = UUID.randomUUID().toString(),
                        docId = doc.id,
                        text = slice,
                        metadata = ChunkMetadata(
                            title = doc.title,
                            file = doc.source,
                            section = currentSection,
                            chunkIndex = chunkIdx++,
                            strategy = "structural"
                        )
                    ))
                    i += maxWordsPerChunk
                }
            }
            currentLines.clear()
        }

        for (line in lines) {
            if (headerRegex.matches(line)) {
                flush()
                currentSection = line.trimStart('#').trim()
                currentLines.add(line)
            } else {
                currentLines.add(line)
            }
        }
        flush()
        return chunks
    }
}
