package com.example.aichallengeapp.core.periodictask.domain.model

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class PeriodicTaskMessage(
    val chatId: String,
    val taskId: String,
    val summary: String,
    val toolName: String,
    val timestamp: Long = System.currentTimeMillis()
)

class PeriodicTaskMessageBus {
    private val _messages = MutableSharedFlow<PeriodicTaskMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<PeriodicTaskMessage> = _messages.asSharedFlow()

    suspend fun emit(message: PeriodicTaskMessage) {
        _messages.emit(message)
    }
}
