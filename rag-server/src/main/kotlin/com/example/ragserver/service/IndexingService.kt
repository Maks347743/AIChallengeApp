package com.example.ragserver.service

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ragserver.chunking.ChunkingStrategy
import com.example.ragserver.data.ChunkStorage
import com.example.ragserver.data.DocumentStorage
import com.example.ragserver.data.VectorIndex
import com.example.ragserver.embedding.OllamaEmbeddingService
import androidx.compose.runtime.mutableStateListOf
import java.nio.file.Path

class IndexingService(
    private val documentStorage: DocumentStorage,
    private val chunkStorage: ChunkStorage,
    private val embeddingService: OllamaEmbeddingService,
    private val vectorIndex: VectorIndex,
    private val indexPath: Path
) {
    val logs = mutableStateListOf<String>()

    var indexedChunks by mutableStateOf(0)
        private set
    var isRunning by mutableStateOf(false)
        private set
    var lastRunTimestamp by mutableStateOf<Long?>(null)
        private set

    suspend fun runIndexing(strategy: ChunkingStrategy) {
        isRunning = true
        try {
            logs.add("Loading documents...")
            val docs = documentStorage.loadAll()
            logs.add("Found ${docs.size} documents.")

            chunkStorage.clear()
            vectorIndex.reset()

            val chunks = docs.flatMap { strategy.chunk(it) }
            logs.add("${chunks.size} chunks created, embedding...")

            var skipped = 0
            chunks.forEachIndexed { i, chunk ->
                val vec = try {
                    embeddingService.embed(chunk.text)
                } catch (e: Exception) {
                    logs.add("[${i + 1}/${chunks.size}] SKIP: ${e.message?.take(120)}")
                    skipped++
                    return@forEachIndexed
                }
                if (vec == null) {
                    logs.add("[${i + 1}/${chunks.size}] SKIP: blank chunk")
                    skipped++
                    return@forEachIndexed
                }
                vectorIndex.add(chunk.id, vec)
                chunkStorage.save(chunk)
                logs.add("[${i + 1}/${chunks.size}] [${chunk.metadata.strategy}#${chunk.metadata.chunkIndex}] ${chunk.metadata.title} | ${chunk.metadata.section ?: "—"}")
            }

            vectorIndex.save(indexPath)
            val indexed = chunks.size - skipped
            indexedChunks = indexed
            lastRunTimestamp = System.currentTimeMillis()
            logs.add("Done. Index saved ($indexed indexed, $skipped skipped).")
        } catch (e: Exception) {
            logs.add("Error: ${e.message}")
        } finally {
            isRunning = false
        }
    }
}
