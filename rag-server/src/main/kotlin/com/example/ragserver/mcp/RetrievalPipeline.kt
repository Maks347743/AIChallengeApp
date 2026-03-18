package com.example.ragserver.mcp

import com.example.ragserver.config.RagConfig
import com.example.ragserver.data.Chunk
import com.example.ragserver.data.ChunkStorage
import com.example.ragserver.data.VectorIndex
import com.example.ragserver.embedding.EmbeddingService
import com.example.ragserver.query.QueryRewriter
import com.example.ragserver.reranking.Reranker
import org.slf4j.LoggerFactory

class RetrievalPipeline(
    private val embeddingService: EmbeddingService,
    private val vectorIndex: VectorIndex,
    private val chunkStorage: ChunkStorage,
    private val queryRewriter: QueryRewriter,
    private val reranker: Reranker,
    private val configProvider: () -> RagConfig
) {
    private val log = LoggerFactory.getLogger(RetrievalPipeline::class.java)

    suspend fun retrieve(query: String, topK: Int): List<Chunk> {
        val config = configProvider()

        val embeddingQuery = if (config.useQueryRewrite && config.deepSeekApiKey.isNotBlank()) {
            queryRewriter.rewrite(query).also { rewritten ->
                log.info("[rewrite] \"$query\" → \"$rewritten\"")
            }
        } else {
            query
        }

        val queryVec = embeddingService.embed(embeddingQuery)
            ?: error("Failed to embed query: Ollama returned no vector")

        val initialK = if (config.useRerank) config.initialK else topK
        val initialIds = vectorIndex.search(queryVec, initialK)
        if (initialIds.isEmpty()) return emptyList()

        val chunks = initialIds.mapNotNull { chunkStorage.load(it) }

        return if (config.useRerank && config.jinaApiKey.isNotBlank()) {
            log.info("[before rerank] top-${chunks.size} by embedding:\n" +
                chunks.mapIndexed { i, c -> "  ${i + 1}. ${c.metadata.section ?: c.metadata.title} (chunk#${c.metadata.chunkIndex})" }
                    .joinToString("\n"))

            val scores = reranker.rerank(query, chunks.map { it.text })
            if (scores.isEmpty()) {
                chunks.take(topK)
            } else {
                val ranked = chunks.zip(scores).sortedByDescending { (_, s) -> s }
                log.info("[after rerank] sorted by relevance (threshold=${config.similarityThreshold}):\n" +
                    ranked.mapIndexed { i, (c, s) ->
                        val mark = if (s >= config.similarityThreshold) "✓" else "✗"
                        "  $mark ${i + 1}. ${"%.3f".format(s)}  ${c.metadata.section ?: c.metadata.title} (chunk#${c.metadata.chunkIndex})"
                    }.joinToString("\n"))

                val result = ranked
                    .filter { (_, s) -> s >= config.similarityThreshold }
                    .take(topK)
                    .map { (c, _) -> c }
                if (result.isEmpty()) {
                    log.info("[threshold result] 0 chunks — all filtered out")
                } else {
                    log.info("[threshold result] ${result.size} chunk(s) returned:\n" +
                        result.mapIndexed { i, c ->
                            "  ${i + 1}. ${c.metadata.section ?: c.metadata.title} (chunk#${c.metadata.chunkIndex})"
                        }.joinToString("\n"))
                }
                result
            }
        } else {
            chunks.take(topK)
        }
    }
}
