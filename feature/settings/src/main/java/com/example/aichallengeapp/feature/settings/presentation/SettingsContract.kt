package com.example.aichallengeapp.feature.settings.presentation

import com.example.aichallengeapp.feature.settings.domain.model.ChatSettings
import com.example.aichallengeapp.feature.settings.domain.model.DeepSeekModel

data class SettingsState(
    val settings: ChatSettings = ChatSettings()
)

sealed interface SettingsIntent {
    data class UpdateMaxTokens(val value: String) : SettingsIntent
    data class UpdateSystemPrompt(val text: String) : SettingsIntent
    data class UpdateTemperature(val value: Float) : SettingsIntent
    data class UpdateModel(val model: DeepSeekModel) : SettingsIntent
}
