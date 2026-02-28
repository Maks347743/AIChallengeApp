package com.example.aichallengeapp.core.database.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
) {
    companion object {
        const val ROLE_SYSTEM = "system"
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val ROLE_SUMMARY = "summary"
        const val ROLE_FACTS = "facts"
    }
}
