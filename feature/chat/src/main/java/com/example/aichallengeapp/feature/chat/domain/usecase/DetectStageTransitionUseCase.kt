package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.TaskStage
import com.example.aichallengeapp.feature.chat.domain.PromptTemplates

class DetectStageTransitionUseCase(
    private val sendChatMessageUseCase: SendChatMessageUseCase
) {
    suspend operator fun invoke(
        existingMessages: List<ChatMessage>,
        newUserMessage: ChatMessage,
        currentStage: TaskStage,
        model: String
    ): TaskStage? {
        val contextMessages = existingMessages
            .filter { it.role == ChatMessage.ROLE_USER || it.role == ChatMessage.ROLE_ASSISTANT }
            .takeLast(4)
        val contextText = buildString {
            contextMessages.forEach { msg ->
                val label = if (msg.role == ChatMessage.ROLE_ASSISTANT) PromptTemplates.ROLE_LABEL_ASSISTANT else PromptTemplates.ROLE_LABEL_USER
                append("$label: ${msg.content}\n")
            }
            append("${PromptTemplates.ROLE_LABEL_USER}: ${newUserMessage.content}\n")
        }
        val result = sendChatMessageUseCase(
            messages = listOf(
                ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = PromptTemplates.stageDetectorPrompt(currentStage)),
                ChatMessage(role = ChatMessage.ROLE_USER, content = contextText)
            ),
            maxTokens = null,
            temperature = null,
            model = model
        )
        val response = result.getOrNull()?.message?.trim() ?: return null
        if (response == "NO_CHANGE") return null
        val proposed = runCatching { TaskStage.valueOf(response) }.getOrNull() ?: return null
        if (!isTransitionAllowed(currentStage, proposed)) return null
        return proposed
    }

    companion object {
        fun isTransitionAllowed(from: TaskStage, to: TaskStage): Boolean = when (from) {
            TaskStage.PLANNING -> to == TaskStage.EXECUTION
            TaskStage.EXECUTION -> to == TaskStage.EVALUATION || to == TaskStage.PLANNING
            TaskStage.EVALUATION -> to == TaskStage.DONE || to == TaskStage.EXECUTION || to == TaskStage.PLANNING
            TaskStage.DONE -> to == TaskStage.PLANNING
        }
    }
}
