package com.example.aichallengeapp.domain.usecase

import com.example.aichallengeapp.domain.model.ChatMessage
import com.example.aichallengeapp.domain.model.ChatResult
import com.example.aichallengeapp.domain.repository.ChatRepository

class SendChatMessageUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(
        messages: List<ChatMessage>,
        maxTokens: Int?,
        temperature: Float?,
        model: String
    ): Result<ChatResult> {
        return chatRepository.sendMessage(messages, maxTokens, temperature, model)
    }
}
