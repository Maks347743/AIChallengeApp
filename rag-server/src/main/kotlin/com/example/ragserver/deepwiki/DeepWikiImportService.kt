package com.example.ragserver.deepwiki

import com.example.ragserver.data.Document
import com.example.ragserver.data.DocumentStorage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DeepWikiImportService(
    private val client: DeepWikiClient,
    private val documentStorage: DocumentStorage
) {
    /**
     * Imports wiki content for a GitHub repo (e.g. "JetBrains/compose-multiplatform").
     * Calls read_wiki_structure for overview, then read_wiki_contents for full docs.
     */
    suspend fun importRepo(
        repoName: String,
        onProgress: suspend (String) -> Unit
    ): List<Document> {
        onProgress("Connecting to DeepWiki for $repoName...")

        val saved = mutableListOf<Document>()
        val args = buildJsonObject { put("repoName", repoName) }

        // Step 1: read_wiki_structure — list of topics
        onProgress("Fetching wiki structure...")
        val structureResult = client.callTool("read_wiki_structure", args)
        if (structureResult.isError == true) {
            onProgress("Structure error: ${structureResult.content.firstOrNull()?.text}")
        } else {
            val text = structureResult.content.mapNotNull { it.text }.joinToString("\n")
            if (text.isNotBlank()) {
                val doc = documentStorage.create(
                    title = "$repoName — Wiki Structure",
                    source = "deepwiki:$repoName/structure",
                    content = text
                )
                saved.add(doc)
                onProgress("Saved structure (${text.length} chars)")
            }
        }

        // Step 2: read_wiki_contents — full documentation
        onProgress("Fetching full wiki contents (this may take a while)...")
        val contentsResult = client.callTool("read_wiki_contents", args)
        if (contentsResult.isError == true) {
            onProgress("Contents error: ${contentsResult.content.firstOrNull()?.text}")
        } else {
            val text = contentsResult.content.mapNotNull { it.text }.joinToString("\n")
            if (text.isNotBlank()) {
                val doc = documentStorage.create(
                    title = "$repoName — Wiki Contents",
                    source = "deepwiki:$repoName/contents",
                    content = text
                )
                saved.add(doc)
                onProgress("Saved full wiki (${text.length} chars)")
            }
        }

        if (saved.isEmpty()) {
            onProgress("No content imported. Check the repo name (format: owner/repo).")
        } else {
            onProgress("Done. Imported ${saved.size} document(s). Go to Indexing tab to vectorize.")
        }

        return saved
    }
}
