package com.example.aichallengeapp.core.database.domain.repository

import com.example.aichallengeapp.core.database.domain.model.ChatMetrics
import kotlinx.coroutines.flow.Flow

interface ChatMetricsRepository {
    fun observeMetrics(chatId: String): Flow<ChatMetrics?>
    suspend fun upsertMetrics(metrics: ChatMetrics)
    suspend fun deleteMetrics(chatId: String)
}
