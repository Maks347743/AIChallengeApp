package com.example.aichallengeapp.core.database.domain.model

data class ResponseMetrics(
    val responseTimeMs: Long,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val costUsd: Double
)
