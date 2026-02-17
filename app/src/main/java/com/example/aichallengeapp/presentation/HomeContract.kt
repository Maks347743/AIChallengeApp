package com.example.aichallengeapp.presentation

import com.example.aichallengeapp.domain.model.ChatMessage

sealed interface HomeIntent {
    data object SendMessage : HomeIntent
    data class UpdateInput(val text: String) : HomeIntent
    data object ClearChat : HomeIntent
    data object ToggleSettings : HomeIntent
    data class UpdateStopWord(val word: String) : HomeIntent
    data class UpdateMaxTokens(val value: String) : HomeIntent
    data class UpdateResponseFormat(val format: ResponseFormat) : HomeIntent
}

enum class ResponseFormat(val label: String) {
    PLAIN_TEXT("простой текст"),
    JSON("Json")
}

data class ChatSettings(
    val stopWord: String = "Хватит",
    val maxTokensText: String = "",
    val responseFormat: ResponseFormat = ResponseFormat.PLAIN_TEXT
) {
    val maxTokens: Int?
        get() = maxTokensText.toIntOrNull()
}

data class HomeState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val showSettings: Boolean = false,
    val settings: ChatSettings = ChatSettings()
)
