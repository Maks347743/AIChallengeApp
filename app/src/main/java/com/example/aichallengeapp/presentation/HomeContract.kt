package com.example.aichallengeapp.presentation

import com.example.aichallengeapp.domain.model.ChatMessage

sealed interface HomeIntent {
    data object SendMessage : HomeIntent
    data class UpdateInput(val text: String) : HomeIntent
    data object ClearChat : HomeIntent
    data class UpdateMaxTokens(val value: String) : HomeIntent
    data class UpdateSystemPrompt(val text: String) : HomeIntent
    data class UpdateTemperature(val value: Float) : HomeIntent
}

data class ChatSettings(
    val systemPrompt: String = "You are a helpful assistant",
    val maxTokensText: String = "",
    val temperature: Float = 1.0f
) {
    val maxTokens: Int?
        get() = maxTokensText.toIntOrNull()
}

data class HomeState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val settings: ChatSettings = ChatSettings()
)
