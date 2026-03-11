package com.example.aichallengeapp.core.database.domain

interface PeriodicTaskServiceController {
    fun ensureStarted()
    fun stop()
}
