package com.example.aichallengeapp.core.database.domain.repository

import com.example.aichallengeapp.core.database.domain.model.PeriodicTask
import com.example.aichallengeapp.core.database.domain.model.PeriodicTaskResult
import kotlinx.coroutines.flow.Flow

interface PeriodicTaskRepository {
    fun getAllActive(): Flow<List<PeriodicTask>>
    suspend fun getAllActiveSuspend(): List<PeriodicTask>
    suspend fun getByChatId(chatId: String): List<PeriodicTask>
    suspend fun getById(id: String): PeriodicTask?
    suspend fun upsert(task: PeriodicTask)
    suspend fun deactivate(taskId: String)
    suspend fun deactivateByChatId(chatId: String)
    suspend fun updateLastExecuted(taskId: String, timestamp: Long)
    suspend fun insertResult(result: PeriodicTaskResult)
    suspend fun getResultsByTaskId(taskId: String): List<PeriodicTaskResult>
}
