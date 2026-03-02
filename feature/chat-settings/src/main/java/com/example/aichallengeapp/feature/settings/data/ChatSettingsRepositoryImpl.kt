package com.example.aichallengeapp.feature.settings.data

import com.example.aichallengeapp.core.database.domain.repository.ChatSessionRepository
import com.example.aichallengeapp.feature.settings.domain.model.ChatSettings
import com.example.aichallengeapp.feature.settings.domain.repository.ChatSettingsRepository
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

class ChatSettingsRepositoryImpl(
    private val sessionRepository: ChatSessionRepository
) : ChatSettingsRepository {

    override suspend fun load(chatId: String): ChatSettings {
        val settingsJson = sessionRepository.getSettingsJson(chatId) ?: return ChatSettings()
        return try {
            json.decodeFromString<ChatSettings>(settingsJson)
        } catch (_: Exception) {
            ChatSettings()
        }
    }

    override suspend fun save(chatId: String, settings: ChatSettings) {
        val settingsJson = json.encodeToString(settings)
        sessionRepository.updateSettingsJson(chatId, settingsJson)
    }
}
