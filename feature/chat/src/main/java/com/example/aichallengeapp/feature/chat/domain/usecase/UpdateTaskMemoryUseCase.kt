package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.feature.settings.domain.model.AppSettings
import com.example.aichallengeapp.feature.settings.domain.model.resolveEndpoint


class UpdateTaskMemoryUseCase(private val sendChatMessageUseCase: SendChatMessageUseCase) {
    suspend operator fun invoke(
        currentMemory: String?,
        recentMessages: List<ChatMessage>,
        appSettings: AppSettings
    ): String {
        val prompt = buildString {
            append("Ты — экстрактор памяти задачи. Обнови память на основе диалога.\n")
            append("Текущая память: ${currentMemory ?: "пусто"}\n\n")
            append("Диалог:\n")
            recentMessages.forEach { append("${it.role}: ${it.content}\n") }
            append("\nВерни ТОЛЬКО краткий текст (3-7 пунктов, тезисно):\n")
            append("Цель: ...\nУточнено: ...\nОграничения: ...")
        }
        val endpoint = appSettings.resolveEndpoint()
        val messages = listOf(ChatMessage(role = ChatMessage.ROLE_USER, content = prompt))
        return sendChatMessageUseCase(
            messages = messages,
            maxTokens = null,
            temperature = 0.3f,
            model = endpoint.modelId,
            tools = null,
            baseUrlOverride = endpoint.baseUrlOverride,
            apiKeyOverride = endpoint.apiKeyOverride,
            think = false
        ).getOrElse { return "" }.message
    }
}
