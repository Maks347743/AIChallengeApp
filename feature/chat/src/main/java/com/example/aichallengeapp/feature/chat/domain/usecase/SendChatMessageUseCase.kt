package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.ChatResult
import com.example.aichallengeapp.core.database.domain.repository.ChatRepository
import com.example.aichallengeapp.core.mcp.model.ToolDefinition

class SendChatMessageUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(
        messages: List<ChatMessage>,
        maxTokens: Int?,
        temperature: Float?,
        model: String,
        tools: List<ToolDefinition>? = null,
        baseUrlOverride: String? = null,
        apiKeyOverride: String? = null
    ): Result<ChatResult> {
        return chatRepository.sendMessage(messages, maxTokens, temperature, model, tools, baseUrlOverride, apiKeyOverride)
    }
}
