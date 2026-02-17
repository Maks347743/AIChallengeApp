package com.example.aichallengeapp.domain.model

data class ChatMessage(
    val role: String,
    val content: String
) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
    }
}
