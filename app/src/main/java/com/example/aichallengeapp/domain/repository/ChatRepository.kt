package com.example.aichallengeapp.domain.repository

import com.example.aichallengeapp.domain.model.ChatMessage
import com.example.aichallengeapp.domain.model.ChatResult

interface ChatRepository {
    suspend fun sendMessage(
        messages: List<ChatMessage>,
        maxTokens: Int?,
        temperature: Float?,
        model: String
    ): Result<ChatResult>
}
