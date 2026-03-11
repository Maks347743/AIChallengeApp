package com.example.aichallengeapp.core.periodictask.domain

interface PeriodicTaskServiceController {
    fun ensureStarted()
    fun stop()
}
