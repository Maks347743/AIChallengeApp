package com.example.aichallengeapp.core.database.domain.model

data class ChatMetrics(
    val chatId: String,
    val lastRequestTokens: Int,
    val lastResponseTokens: Int,
    val totalTokens: Int
)
