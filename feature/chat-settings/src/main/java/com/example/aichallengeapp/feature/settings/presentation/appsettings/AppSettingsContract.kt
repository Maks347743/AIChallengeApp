package com.example.aichallengeapp.feature.settings.presentation.appsettings

import com.example.aichallengeapp.feature.settings.domain.model.AppSettings
import com.example.aichallengeapp.feature.settings.domain.model.DeepSeekModel

data class AppSettingsState(
    val settings: AppSettings = AppSettings()
)

sealed interface AppSettingsIntent {
    data class UpdateModel(val model: DeepSeekModel) : AppSettingsIntent
    data class UpdateOllamaModelName(val value: String) : AppSettingsIntent
    data class UpdateServerBaseUrl(val value: String) : AppSettingsIntent
    data class UpdateMcpServerToken(val value: String) : AppSettingsIntent
}
