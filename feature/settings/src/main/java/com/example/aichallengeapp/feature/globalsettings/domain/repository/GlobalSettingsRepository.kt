package com.example.aichallengeapp.feature.globalsettings.domain.repository

import com.example.aichallengeapp.feature.globalsettings.domain.model.GlobalSettings

interface GlobalSettingsRepository {
    suspend fun load(): GlobalSettings
    suspend fun save(settings: GlobalSettings)
}
