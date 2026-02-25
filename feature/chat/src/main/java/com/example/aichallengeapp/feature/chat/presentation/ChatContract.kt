package com.example.aichallengeapp.feature.chat.presentation

import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.ChatMetrics

sealed interface ChatIntent {
    data object SendMessage : ChatIntent
    data class UpdateInput(val text: String) : ChatIntent
    data object ClearChat : ChatIntent
    data object ToggleMetrics : ChatIntent
}

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val chatMetrics: ChatMetrics? = null,
    val showMetrics: Boolean = false
)
