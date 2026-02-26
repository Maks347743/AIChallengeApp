package com.example.aichallengeapp.feature.settings.data

import android.content.SharedPreferences
import com.example.aichallengeapp.feature.settings.domain.model.ChatSettings
import com.example.aichallengeapp.feature.settings.domain.model.DeepSeekModel
import com.example.aichallengeapp.feature.settings.domain.repository.SettingsRepository
import androidx.core.content.edit

class SettingsStorage(private val prefs: SharedPreferences) : SettingsRepository {

    override fun load(): ChatSettings {
        val modelId = prefs.getString(KEY_MODEL, null)
        val model = DeepSeekModel.entries.firstOrNull { it.id == modelId } ?: ChatSettings().model
        val defaults = ChatSettings()
        return ChatSettings(
            systemPrompt = prefs.getString(KEY_SYSTEM_PROMPT, null) ?: defaults.systemPrompt,
            maxTokensText = prefs.getString(KEY_MAX_TOKENS, null) ?: "",
            temperature = prefs.getFloat(KEY_TEMPERATURE, defaults.temperature),
            model = model,
            summaryEnabled = prefs.getBoolean(KEY_SUMMARY_ENABLED, defaults.summaryEnabled),
            maxRecentMessages = prefs.getInt(KEY_SUMMARY_RECENT_MESSAGES, defaults.maxRecentMessages),
            summaryMaxTokens = prefs.getInt(KEY_SUMMARY_MAX_TOKENS, defaults.summaryMaxTokens)
        )
    }

    override fun save(settings: ChatSettings) {
        prefs.edit {
            putString(KEY_SYSTEM_PROMPT, settings.systemPrompt)
                .putString(KEY_MAX_TOKENS, settings.maxTokensText)
                .putFloat(KEY_TEMPERATURE, settings.temperature)
                .putString(KEY_MODEL, settings.model.id)
                .putBoolean(KEY_SUMMARY_ENABLED, settings.summaryEnabled)
                .putInt(KEY_SUMMARY_RECENT_MESSAGES, settings.maxRecentMessages)
                .putInt(KEY_SUMMARY_MAX_TOKENS, settings.summaryMaxTokens)
        }
    }

    private companion object {
        const val KEY_SYSTEM_PROMPT = "system_prompt"
        const val KEY_MAX_TOKENS = "max_tokens"
        const val KEY_TEMPERATURE = "temperature"
        const val KEY_MODEL = "model"
        const val KEY_SUMMARY_ENABLED = "summary_enabled"
        const val KEY_SUMMARY_RECENT_MESSAGES = "summary_recent_messages"
        const val KEY_SUMMARY_MAX_TOKENS = "summary_max_tokens"
    }
}
