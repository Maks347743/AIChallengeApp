package com.example.aichallengeapp.feature.settings.domain.repository

import com.example.aichallengeapp.feature.settings.domain.model.ChatSettings

interface ChatSettingsRepository {
    suspend fun load(chatId: String): ChatSettings
    suspend fun save(chatId: String, settings: ChatSettings)
}
