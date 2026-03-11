package com.example.aichallengeapp.core.periodictask.domain.model

data class PeriodicTask(
    val id: String,
    val chatId: String,
    val toolName: String,
    val toolArgumentsJson: String,
    val intervalMinutes: Int,
    val isActive: Boolean = true,
    val lastExecutedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val prompt: String
)

data class PeriodicTaskResult(
    val id: String,
    val taskId: String,
    val result: String,
    val summary: String,
    val createdAt: Long = System.currentTimeMillis()
)
