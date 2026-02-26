package com.example.aichallengeapp.feature.settings.domain.model

data class ChatSettings(
    val systemPrompt: String = "You are a helpful assistant",
    val maxTokensText: String = "",
    val temperature: Float = 1.0f,
    val model: DeepSeekModel = DeepSeekModel.DEEPSEEK_CHAT,
    val summaryEnabled: Boolean = false,
    val maxRecentMessages: Int = 10,
    val summaryMaxTokens: Int = 50
) {
    val maxTokens: Int?
        get() = maxTokensText.toIntOrNull()
}
