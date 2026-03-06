package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.TaskStage
import com.example.aichallengeapp.feature.chat.domain.PromptTemplates

class DetectNewTaskUseCase(
    private val sendChatMessageUseCase: SendChatMessageUseCase
) {
    suspend operator fun invoke(
        existingMessages: List<ChatMessage>,
        newUserMessage: ChatMessage,
        currentStage: TaskStage,
        model: String
    ): String? {
        val contextMessages = existingMessages
            .filter { it.role == ChatMessage.ROLE_USER || it.role == ChatMessage.ROLE_ASSISTANT }
            .takeLast(3)
        val contextText = buildString {
            append("Текущий этап: ${currentStage.name}\n\n")
            contextMessages.forEach { msg ->
                val label = if (msg.role == ChatMessage.ROLE_ASSISTANT) PromptTemplates.ROLE_LABEL_ASSISTANT else PromptTemplates.ROLE_LABEL_USER
                append("$label: ${msg.content}\n")
            }
            append("${PromptTemplates.ROLE_LABEL_USER}: ${newUserMessage.content}\n")
        }
        val result = sendChatMessageUseCase(
            messages = listOf(
                ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = PromptTemplates.TASK_DETECTOR_SYSTEM_PROMPT),
                ChatMessage(role = ChatMessage.ROLE_USER, content = contextText)
            ),
            maxTokens = null,
            temperature = null,
            model = model
        )
        return result.getOrNull()?.message?.trim()
            ?.takeIf { it.isNotBlank() && it != "NO_CHANGE" }
    }
}
