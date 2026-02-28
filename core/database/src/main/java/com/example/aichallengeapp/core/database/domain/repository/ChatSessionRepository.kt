package com.example.aichallengeapp.core.database.domain.repository

import com.example.aichallengeapp.core.database.domain.model.ChatSession
import kotlinx.coroutines.flow.Flow

interface ChatSessionRepository {
    fun getAllSessions(): Flow<List<ChatSession>>
    suspend fun getSession(id: String): ChatSession?
    suspend fun upsertSession(session: ChatSession)
    suspend fun deleteSession(id: String)
    suspend fun getSettingsJson(chatId: String): String?
    suspend fun updateSettingsJson(chatId: String, settingsJson: String)
    suspend fun getSessionsByGroup(groupId: String): List<ChatSession>
    suspend fun updateCheckpointFields(id: String, groupId: String, branchIndex: Int)
}
