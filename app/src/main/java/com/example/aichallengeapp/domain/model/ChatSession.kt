package com.example.aichallengeapp.domain.model

private const val CHAT_SESSION_TITLE_WORDS_COUNT = 8

data class ChatSession(
    val id: String,
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun previewText(wordCount: Int = CHAT_SESSION_TITLE_WORDS_COUNT): String {
        val last = messages.lastOrNull { it.role != "system" } ?: return "New Chat"
        val words = last.content.trim().split("\\s+".toRegex())
        return if (words.size <= wordCount) last.content.trim()
               else words.take(wordCount).joinToString(" ") + "…"
    }
}
