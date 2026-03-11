package com.example.aichallengeapp.core.database.data.repository

import com.example.aichallengeapp.core.database.data.db.PeriodicTaskDao
import com.example.aichallengeapp.core.database.data.db.PeriodicTaskEntity
import com.example.aichallengeapp.core.database.data.db.PeriodicTaskResultEntity
import com.example.aichallengeapp.core.database.domain.model.PeriodicTask
import com.example.aichallengeapp.core.database.domain.model.PeriodicTaskResult
import com.example.aichallengeapp.core.database.domain.repository.PeriodicTaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PeriodicTaskRepositoryImpl(
    private val dao: PeriodicTaskDao
) : PeriodicTaskRepository {

    override fun getAllActive(): Flow<List<PeriodicTask>> =
        dao.getAllActive().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllActiveSuspend(): List<PeriodicTask> =
        dao.getAllActiveSuspend().map { it.toDomain() }

    override suspend fun getByChatId(chatId: String): List<PeriodicTask> =
        dao.getByChatId(chatId).map { it.toDomain() }

    override suspend fun getById(id: String): PeriodicTask? =
        dao.getById(id)?.toDomain()

    override suspend fun upsert(task: PeriodicTask) =
        dao.upsert(task.toEntity())

    override suspend fun deactivate(taskId: String) =
        dao.deactivate(taskId)

    override suspend fun deactivateByChatId(chatId: String) =
        dao.deactivateByChatId(chatId)

    override suspend fun updateLastExecuted(taskId: String, timestamp: Long) =
        dao.updateLastExecuted(taskId, timestamp)

    override suspend fun insertResult(result: PeriodicTaskResult) =
        dao.insertResult(result.toEntity())

    override suspend fun getResultsByTaskId(taskId: String): List<PeriodicTaskResult> =
        dao.getResultsByTaskId(taskId).map { it.toDomain() }

    private fun PeriodicTaskEntity.toDomain() = PeriodicTask(
        id = id,
        chatId = chatId,
        toolName = toolName,
        toolArgumentsJson = toolArgumentsJson,
        intervalMinutes = intervalMinutes,
        isActive = isActive,
        lastExecutedAt = lastExecutedAt,
        createdAt = createdAt,
        prompt = prompt
    )

    private fun PeriodicTask.toEntity() = PeriodicTaskEntity(
        id = id,
        chatId = chatId,
        toolName = toolName,
        toolArgumentsJson = toolArgumentsJson,
        intervalMinutes = intervalMinutes,
        isActive = isActive,
        lastExecutedAt = lastExecutedAt,
        createdAt = createdAt,
        prompt = prompt
    )

    private fun PeriodicTaskResultEntity.toDomain() = PeriodicTaskResult(
        id = id,
        taskId = taskId,
        result = result,
        summary = summary,
        createdAt = createdAt
    )

    private fun PeriodicTaskResult.toEntity() = PeriodicTaskResultEntity(
        id = id,
        taskId = taskId,
        result = result,
        summary = summary,
        createdAt = createdAt
    )
}
