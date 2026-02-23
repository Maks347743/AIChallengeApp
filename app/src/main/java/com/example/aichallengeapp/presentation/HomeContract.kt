package com.example.aichallengeapp.presentation

import com.example.aichallengeapp.domain.model.ChatMessage
import com.example.aichallengeapp.domain.model.ResponseMetrics

enum class DeepSeekModel(val id: String, val displayName: String) {
    DEEPSEEK_CHAT("deepseek-chat", "DeepSeek Chat"),
    DEEPSEEK_REASONER("deepseek-reasoner", "DeepSeek Reasoner")
}

sealed interface HomeIntent {
    data object SendMessage : HomeIntent
    data class UpdateInput(val text: String) : HomeIntent
    data object ClearChat : HomeIntent
    data class UpdateMaxTokens(val value: String) : HomeIntent
    data class UpdateSystemPrompt(val text: String) : HomeIntent
    data class UpdateTemperature(val value: Float) : HomeIntent
    data class UpdateModel(val model: DeepSeekModel) : HomeIntent
}

data class ChatSettings(
    val systemPrompt: String = "You are a helpful assistant",
    val maxTokensText: String = "",
    val temperature: Float = 1.0f,
    val model: DeepSeekModel = DeepSeekModel.DEEPSEEK_CHAT
) {
    val maxTokens: Int?
        get() = maxTokensText.toIntOrNull()
}

data class HomeState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val settings: ChatSettings = ChatSettings(),
    val lastMetrics: ResponseMetrics? = null
)
