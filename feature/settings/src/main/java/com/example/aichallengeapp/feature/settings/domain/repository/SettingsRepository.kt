package com.example.aichallengeapp.feature.settings.domain.repository

import com.example.aichallengeapp.feature.settings.domain.model.ChatSettings

interface SettingsRepository {
    fun load(): ChatSettings
    fun save(settings: ChatSettings)
}
