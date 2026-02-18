package com.example.aichallengeapp.domain.repository

import com.example.aichallengeapp.domain.model.ChatMessage

interface ChatRepository {
    suspend fun sendMessage(
        messages: List<ChatMessage>,
        maxTokens: Int?,
        temperature: Float?
    ): Result<String>
}
