package com.example.aichallengeapp.presentation

import com.example.aichallengeapp.domain.model.ChatMessage
import com.example.aichallengeapp.domain.model.ResponseMetrics

enum class DeepSeekModel(val id: String, val displayName: String) {
    DEEPSEEK_CHAT("deepseek-chat", "DeepSeek Chat"),
    DEEPSEEK_REASONER("deepseek-reasoner", "DeepSeek Reasoner")
}

sealed interface ChatIntent {
    data object SendMessage : ChatIntent
    data class UpdateInput(val text: String) : ChatIntent
    data object ClearChat : ChatIntent
    data object ToggleMetrics : ChatIntent
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

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastMetrics: ResponseMetrics? = null,
    val showMetrics: Boolean = false
)
