package com.example.aichallengeapp.feature.settings.domain.model

import kotlinx.serialization.Serializable

private const val DEFAULT_SYSTEM_PROMPT = ""
private const val DEFAULT_TEMPERATURE = 1.0f
private const val DEFAULT_RETAINED_MESSAGE_COUNT = 10
private const val DEFAULT_SUMMARY_MAX_TOKENS = 50
private const val DEFAULT_SLIDING_WINDOW_SIZE = 10
private const val DEFAULT_STICKY_FACTS_RECENT_MESSAGES = 10

@Serializable
data class ChatSettings(
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val maxTokens: Int? = null,
    val temperature: Float = DEFAULT_TEMPERATURE,
    val model: DeepSeekModel = DeepSeekModel.DEEPSEEK_CHAT,
    val summaryEnabled: Boolean = false,
    val retainedMessageCount: Int = DEFAULT_RETAINED_MESSAGE_COUNT,
    val summaryMaxTokens: Int = DEFAULT_SUMMARY_MAX_TOKENS,
    val slidingWindowEnabled: Boolean = false,
    val slidingWindowSize: Int = DEFAULT_SLIDING_WINDOW_SIZE,
    val stickyFactsEnabled: Boolean = false,
    val stickyFactsRecentMessages: Int = DEFAULT_STICKY_FACTS_RECENT_MESSAGES,
    val ragEnabled: Boolean = true,
)
