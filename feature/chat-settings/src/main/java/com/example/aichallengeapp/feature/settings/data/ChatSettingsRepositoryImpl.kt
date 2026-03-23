package com.example.aichallengeapp.feature.settings.data

import com.example.aichallengeapp.core.database.domain.repository.ChatSessionRepository
import com.example.aichallengeapp.feature.settings.domain.model.ChatSettings
import com.example.aichallengeapp.feature.settings.domain.repository.ChatSettingsRepository
import kotlinx.serialization.json.Json

class ChatSettingsRepositoryImpl(
    private val sessionRepository: ChatSessionRepository,
    private val json: Json,
    private val defaultOllamaBaseUrl: String = "http://10.0.2.2:11434/v1"
) : ChatSettingsRepository {

    private fun defaultSettings() = ChatSettings(ollamaBaseUrl = defaultOllamaBaseUrl)

    override suspend fun load(chatId: String): ChatSettings {
        val settingsJson = sessionRepository.getSettingsJson(chatId) ?: return defaultSettings()
        return try {
            json.decodeFromString<ChatSettings>(settingsJson)
        } catch (_: Exception) {
            defaultSettings()
        }
    }

    override suspend fun save(chatId: String, settings: ChatSettings) {
        val settingsJson = json.encodeToString(settings)
        sessionRepository.updateSettingsJson(chatId, settingsJson)
    }
}
