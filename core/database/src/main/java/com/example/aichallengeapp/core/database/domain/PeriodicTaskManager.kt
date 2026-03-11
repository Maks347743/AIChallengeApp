package com.example.aichallengeapp.core.database.domain

import com.example.aichallengeapp.core.database.domain.repository.PeriodicTaskRepository

class PeriodicTaskManager(
    private val repository: PeriodicTaskRepository,
    private val serviceController: PeriodicTaskServiceController
) {
    fun onTaskCreated() {
        serviceController.ensureStarted()
    }

    suspend fun onTaskStopped() {
        val activeTasks = repository.getAllActiveSuspend()
        if (activeTasks.isEmpty()) {
            serviceController.stop()
        }
    }

    suspend fun resumeIfNeeded() {
        if (repository.getAllActiveSuspend().isNotEmpty()) {
            serviceController.ensureStarted()
        }
    }
}
