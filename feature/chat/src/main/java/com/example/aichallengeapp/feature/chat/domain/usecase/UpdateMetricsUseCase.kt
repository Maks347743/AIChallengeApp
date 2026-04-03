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
        require(currentTotalTokens >= 0) { "currentTotalTokens must be non-negative" }
        require(responseMetrics.promptTokens >= 0) { "promptTokens must be non-negative" }
        require(responseMetrics.completionTokens >= 0) { "completionTokens must be non-negative" }

        val newTotal = calculateNewTotal(currentTotalTokens, responseMetrics)
        metricsRepository.upsertMetrics(
            ChatMetrics(
                chatId = chatId,
                lastRequestTokens = responseMetrics.promptTokens,
                lastResponseTokens = responseMetrics.completionTokens,
                totalTokens = newTotal
            )
        )
    }

    private fun calculateNewTotal(current: Int, metrics: ResponseMetrics): Int =
        current + metrics.promptTokens + metrics.completionTokens
}
