package com.example.ragserver.data

import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.io.path.writeText

class ChunkStorage(private val chunksPath: Path, private val json: Json) {

    private val cache = ConcurrentHashMap<String, Chunk>()

    fun save(chunk: Chunk) {
        cache[chunk.id] = chunk
        chunksPath.resolve("${chunk.id}.json").writeText(
            json.encodeToString(Chunk.serializer(), chunk)
        )
    }

    fun load(id: String): Chunk? {
        cache[id]?.let { return it }
        val file = chunksPath.resolve("$id.json")
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString(Chunk.serializer(), file.readText()).also { cache[it.id] = it }
        }.getOrNull()
    }

    fun clear() {
        cache.clear()
        chunksPath.listDirectoryEntries("*.json").forEach { it.deleteIfExists() }
    }
}
