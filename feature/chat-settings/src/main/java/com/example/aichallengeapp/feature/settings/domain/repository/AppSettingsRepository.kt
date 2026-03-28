package com.example.aichallengeapp.feature.settings.domain.repository

import com.example.aichallengeapp.feature.settings.domain.model.AppSettings

interface AppSettingsRepository {
    suspend fun load(): AppSettings
    suspend fun save(settings: AppSettings)
}
