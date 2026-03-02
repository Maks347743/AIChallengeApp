package com.example.aichallengeapp.feature.globalsettings.presentation

data class GlobalSettingsState(val systemPromptPrefix: String = "")

sealed interface GlobalSettingsIntent {
    data class UpdatePrefix(val text: String) : GlobalSettingsIntent
}
