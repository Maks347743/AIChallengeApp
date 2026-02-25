package com.example.aichallengeapp.core.database.data.repository

import com.example.aichallengeapp.core.database.data.db.ChatMetricsDao
import com.example.aichallengeapp.core.database.data.db.ChatMetricsEntity
import com.example.aichallengeapp.core.database.domain.model.ChatMetrics
import com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatMetricsRepositoryImpl(private val dao: ChatMetricsDao) : ChatMetricsRepository {

    override fun observeMetrics(chatId: String): Flow<ChatMetrics?> =
        dao.observe(chatId).map { it?.toDomain() }

    override suspend fun upsertMetrics(metrics: ChatMetrics) =
        dao.upsert(metrics.toEntity())

    override suspend fun deleteMetrics(chatId: String) =
        dao.delete(chatId)

    private fun ChatMetricsEntity.toDomain() = ChatMetrics(
        chatId = chatId,
        lastRequestTokens = lastRequestTokens,
        lastResponseTokens = lastResponseTokens,
        totalTokens = totalTokens
    )

    private fun ChatMetrics.toEntity() = ChatMetricsEntity(
        chatId = chatId,
        lastRequestTokens = lastRequestTokens,
        lastResponseTokens = lastResponseTokens,
        totalTokens = totalTokens
    )
}
