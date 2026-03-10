package com.example.aichallengeapp.feature.settings.presentation

import com.example.aichallengeapp.feature.settings.domain.model.ChatSettings
import com.example.aichallengeapp.feature.settings.domain.model.DeepSeekModel

private val DEFAULTS = ChatSettings()

data class SettingsState(
    val settings: ChatSettings = DEFAULTS,
    val maxTokensText: String = "",
    val maxRecentMessagesText: String = DEFAULTS.retainedMessageCount.toString(),
    val summaryMaxTokensText: String = DEFAULTS.summaryMaxTokens.toString(),
    val slidingWindowSizeText: String = DEFAULTS.slidingWindowSize.toString(),
    val stickyFactsRecentMessagesText: String = DEFAULTS.stickyFactsRecentMessages.toString(),
)

sealed interface SettingsIntent {
    data class UpdateMaxTokens(val value: String) : SettingsIntent
    data class UpdateSystemPrompt(val text: String) : SettingsIntent
    data class UpdateTemperature(val value: Float) : SettingsIntent
    data class UpdateModel(val model: DeepSeekModel) : SettingsIntent
    data class ToggleSummary(val enabled: Boolean) : SettingsIntent
    data class UpdateSummaryRecentMessages(val value: String) : SettingsIntent
    data class UpdateSummaryMaxTokens(val value: String) : SettingsIntent
    data class ToggleSlidingWindow(val enabled: Boolean) : SettingsIntent
    data class UpdateSlidingWindowSize(val value: String) : SettingsIntent
    data class ToggleStickyFacts(val enabled: Boolean) : SettingsIntent
    data class UpdateStickyFactsRecentMessages(val value: String) : SettingsIntent
}
