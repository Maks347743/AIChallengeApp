package com.example.aichallengeapp.data.repository

import com.example.aichallengeapp.data.db.ChatSessionDao
import com.example.aichallengeapp.data.db.ChatSessionEntity
import com.example.aichallengeapp.domain.model.ChatSession
import com.example.aichallengeapp.domain.repository.ChatSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        dao.getById(id)?.let { dao.delete(it) }
    }

    private fun ChatSessionEntity.toDomain() = ChatSession(
        id = id,
        messages = messages,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun ChatSession.toEntity() = ChatSessionEntity(
        id = id,
        messages = messages,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
