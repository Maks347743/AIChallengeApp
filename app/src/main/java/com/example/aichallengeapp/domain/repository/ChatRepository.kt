package com.example.aichallengeapp.domain.repository

interface ChatRepository {
    suspend fun sendMessage(message: String): Result<String>
}
