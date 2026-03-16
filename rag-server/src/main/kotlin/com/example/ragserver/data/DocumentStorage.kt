package com.example.ragserver.data

import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.io.path.writeText

class DocumentStorage(private val docsPath: Path) {

    private val json = Json { ignoreUnknownKeys = true }

    fun save(doc: Document) {
        docsPath.resolve("${doc.id}.json").writeText(
            json.encodeToString(Document.serializer(), doc)
        )
    }

    fun loadAll(): List<Document> {
        return docsPath.listDirectoryEntries("*.json").mapNotNull { file ->
            runCatching {
                json.decodeFromString(Document.serializer(), file.readText())
            }.getOrNull()
        }.sortedBy { it.createdAt }
    }

    fun pathOf(id: String): String = docsPath.resolve("$id.json").toAbsolutePath().toString()

    fun delete(id: String) {
        docsPath.resolve("$id.json").deleteIfExists()
    }

    fun create(title: String, source: String, content: String): Document {
        val doc = Document(
            id = UUID.randomUUID().toString(),
            title = title,
            source = source,
            content = content
        )
        save(doc)
        return doc
    }
}
