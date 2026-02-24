package com.example.aichallengeapp.domain.repository

import com.example.aichallengeapp.domain.model.ChatSession
import kotlinx.coroutines.flow.Flow

interface ChatSessionRepository {
    fun getAllSessions(): Flow<List<ChatSession>>
    suspend fun getSession(id: String): ChatSession?
    suspend fun upsertSession(session: ChatSession)
    suspend fun deleteSession(id: String)
}
