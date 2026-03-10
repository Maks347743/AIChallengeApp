package com.example.aichallengeapp.core.database.data.repository

import com.example.aichallengeapp.core.database.data.db.ChatSessionDao
import com.example.aichallengeapp.core.database.data.db.ChatSessionEntity
import com.example.aichallengeapp.core.database.domain.model.ChatSession
import com.example.aichallengeapp.core.database.domain.model.TaskStage
import com.example.aichallengeapp.core.database.domain.repository.ChatSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

class ChatSessionRepositoryImpl(
    private val dao: ChatSessionDao
) : ChatSessionRepository {

    override fun getAllSessions(): Flow<List<ChatSession>> =
        dao.getAllSessions().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getSession(id: String): ChatSession? =
        dao.getById(id)?.toDomain()

    override suspend fun upsertSession(session: ChatSession) =
        dao.upsert(session.toEntity())

    override suspend fun deleteSession(id: String) {
        dao.deleteById(id)
    }

    override suspend fun getSettingsJson(chatId: String): String? =
        dao.getById(chatId)?.settingsJson

    override suspend fun updateSettingsJson(chatId: String, settingsJson: String) {
        val rows = dao.updateSettingsJson(chatId, settingsJson)
        if (rows == 0) {
            val now = System.currentTimeMillis()
            dao.upsert(
                ChatSessionEntity(
                    id = chatId,
                    messages = emptyList(),
                    createdAt = now,
                    updatedAt = now,
                    settingsJson = settingsJson
                )
            )
        }
    }

    override suspend fun getSessionsByGroup(groupId: String): List<ChatSession> =
        dao.getSessionsByGroup(groupId).map { it.toDomain() }

    override suspend fun updateCheckpointFields(id: String, groupId: String, branchIndex: Int) {
        dao.updateCheckpointFields(id, groupId, branchIndex)
    }

    private fun ChatSessionEntity.toDomain() = ChatSession(
        id = id,
        messages = messages,
        createdAt = createdAt,
        updatedAt = updatedAt,
        settingsJson = settingsJson,
        checkpointGroupId = checkpointGroupId,
        branchIndex = branchIndex,
        currentTask = currentTask,
        currentTaskStage = currentTaskStage?.let { runCatching { TaskStage.valueOf(it) }.getOrNull() } ?: TaskStage.PLANNING,
        profileId = profileId,
        stageArtifacts = stageArtifactsJson
            ?.let { json.decodeFromString<Map<String, String>>(it) }
            ?.mapKeys { (k, _) -> runCatching { TaskStage.valueOf(k) }.getOrNull() }
            ?.filterKeys { it != null }
            ?.mapKeys { (k, _) -> k!! }
            ?: emptyMap()
    )

    private fun ChatSession.toEntity() = ChatSessionEntity(
        id = id,
        messages = messages,
        createdAt = createdAt,
        updatedAt = updatedAt,
        settingsJson = settingsJson,
        checkpointGroupId = checkpointGroupId,
        branchIndex = branchIndex,
        currentTask = currentTask,
        currentTaskStage = currentTaskStage.name,
        profileId = profileId,
        stageArtifactsJson = stageArtifacts
            .takeIf { it.isNotEmpty() }
            ?.let { json.encodeToString(it.mapKeys { (k, _) -> k.name }) }
    )
}
