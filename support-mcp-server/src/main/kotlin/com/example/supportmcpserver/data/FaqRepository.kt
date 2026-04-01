package com.example.supportmcpserver.data

import java.sql.Connection
import java.sql.ResultSet

data class FaqEntry(
    val id: String,
    val question: String,
    val answer: String,
    val tags: List<String>
)

class FaqRepository(private val connection: Connection) {

    private val entries: List<FaqEntry> by lazy {
        connection.prepareStatement("SELECT * FROM faq").use { ps ->
            ps.executeQuery().toList()
        }
    }

    fun search(query: String, maxResults: Int = 3): List<FaqEntry> {
        val terms = query.lowercase().split(Regex("\\s+")).filter { it.length > 2 }
        if (terms.isEmpty()) return entries.take(maxResults)

        return entries
            .map { entry ->
                val blob = (entry.question + " " + entry.answer + " " + entry.tags.joinToString(" ")).lowercase()
                val score = terms.count { term -> blob.contains(term) }
                entry to score
            }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .take(maxResults)
            .map { (entry, _) -> entry }
    }

    private fun ResultSet.toList(): List<FaqEntry> {
        val result = mutableListOf<FaqEntry>()
        while (next()) result.add(toFaqEntry())
        return result
    }

    private fun ResultSet.toFaqEntry() = FaqEntry(
        id       = getString("id"),
        question = getString("question"),
        answer   = getString("answer"),
        tags     = getString("tags").split(",").map { it.trim() }
    )
}
