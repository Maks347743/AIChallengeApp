package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.TaskStage
import com.example.aichallengeapp.feature.chat.domain.PromptTemplates

class GenerateStageArtifactUseCase(
    private val sendChatMessageUseCase: SendChatMessageUseCase
) {
    suspend operator fun invoke(
        stage: TaskStage,
        messages: List<ChatMessage>,
        model: String
    ): String {
        val extractorPrompt = PromptTemplates.artifactExtractorPrompt(stage)
        if (extractorPrompt.isEmpty()) return ""
        val conversationText = messages
            .filter { it.role == ChatMessage.ROLE_USER || it.role == ChatMessage.ROLE_ASSISTANT }
            .joinToString("\n") { msg ->
                val label = if (msg.role == ChatMessage.ROLE_ASSISTANT) PromptTemplates.ROLE_LABEL_ASSISTANT else PromptTemplates.ROLE_LABEL_USER
                "$label: ${msg.content}"
            }
        val result = sendChatMessageUseCase(
            messages = listOf(
                ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = extractorPrompt),
                ChatMessage(role = ChatMessage.ROLE_USER, content = conversationText)
            ),
            maxTokens = null,
            temperature = null,
            model = model
        )
        return result.getOrNull()?.message?.trim() ?: ""
    }
}
