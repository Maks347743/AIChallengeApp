package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.repository.ChatRepository

class UpdateTaskMemoryUseCase(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(
        currentMemory: String?,
        recentMessages: List<ChatMessage>
    ): String {
        val prompt = buildString {
            append("Ты — экстрактор памяти задачи. Обнови память на основе диалога.\n")
            append("Текущая память: ${currentMemory ?: "пусто"}\n\n")
            append("Диалог:\n")
            recentMessages.forEach { append("${it.role}: ${it.content}\n") }
            append("\nВерни ТОЛЬКО краткий текст (3-7 пунктов, тезисно):\n")
            append("Цель: ...\nУточнено: ...\nОграничения: ...")
        }
        return chatRepository.sendRawMessage(prompt)
    }
}
