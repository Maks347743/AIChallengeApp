package com.example.ragserver.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.math.sqrt

class VectorIndex {
    private val vectors = ConcurrentHashMap<String, FloatArray>()

    fun add(chunkId: String, vector: FloatArray) {
        vectors[chunkId] = vector
    }

    fun search(queryVector: FloatArray, k: Int = 5): List<String> {
        if (vectors.isEmpty()) return emptyList()
        return vectors.entries
            .map { (id, vec) -> id to cosineSimilarity(queryVector, vec) }
            .sortedByDescending { it.second }
            .take(k)
            .map { it.first }
    }

    fun save(path: Path) {
        path.toFile().parentFile?.mkdirs()
        val data = IndexData(vectors.mapValues { it.value.toList() })
        path.writeText(Json.encodeToString(IndexData.serializer(), data))
    }

    fun load(path: Path): Boolean {
        if (!path.exists()) return false
        return runCatching {
            val data = Json.decodeFromString(IndexData.serializer(), path.readText())
            vectors.clear()
            data.vectors.forEach { (id, vec) -> vectors[id] = vec.toFloatArray() }
            true
        }.getOrDefault(false)
    }

    fun reset() {
        vectors.clear()
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0f) 0f else dot / denom
    }

    @Serializable
    private data class IndexData(val vectors: Map<String, List<Float>>)
}
