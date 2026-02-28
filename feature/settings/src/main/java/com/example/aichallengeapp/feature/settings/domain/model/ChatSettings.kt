package com.example.aichallengeapp.feature.settings.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatSettings(
    val systemPrompt: String = "You are a helpful assistant",
    val maxTokensText: String = "",
    val temperature: Float = 1.0f,
    val model: DeepSeekModel = DeepSeekModel.DEEPSEEK_CHAT,
    val summaryEnabled: Boolean = false,
    val retainedMessageCount: Int = 10,
    val summaryMaxTokens: Int = 50,
    val slidingWindowEnabled: Boolean = false,
    val slidingWindowSize: Int = 10,
    val stickyFactsEnabled: Boolean = false,
    val stickyFactsRecentMessages: Int = 10,
) {
    val maxTokens: Int?
        get() = maxTokensText.toIntOrNull()
}
