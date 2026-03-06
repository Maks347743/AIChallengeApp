package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.ChatMetrics
import com.example.aichallengeapp.core.database.domain.model.ResponseMetrics
import com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository

class UpdateMetricsUseCase(
    private val metricsRepository: ChatMetricsRepository
) {
    suspend operator fun invoke(
        chatId: String,
        currentTotalTokens: Int,
        responseMetrics: ResponseMetrics
    ) {
        val newTotal = currentTotalTokens + responseMetrics.promptTokens + responseMetrics.completionTokens
        metricsRepository.upsertMetrics(
            ChatMetrics(
                chatId = chatId,
                lastRequestTokens = responseMetrics.promptTokens,
                lastResponseTokens = responseMetrics.completionTokens,
                totalTokens = newTotal
            )
        )
    }
}
