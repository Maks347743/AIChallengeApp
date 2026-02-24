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
        return ChatSettings(
            systemPrompt = prefs.getString(KEY_SYSTEM_PROMPT, null) ?: ChatSettings().systemPrompt,
            maxTokensText = prefs.getString(KEY_MAX_TOKENS, null) ?: "",
            temperature = prefs.getFloat(KEY_TEMPERATURE, ChatSettings().temperature),
            model = model
        )
    }

    override fun save(settings: ChatSettings) {
        prefs.edit {
            putString(KEY_SYSTEM_PROMPT, settings.systemPrompt)
                .putString(KEY_MAX_TOKENS, settings.maxTokensText)
                .putFloat(KEY_TEMPERATURE, settings.temperature)
                .putString(KEY_MODEL, settings.model.id)
        }
    }

    private companion object {
        const val KEY_SYSTEM_PROMPT = "system_prompt"
        const val KEY_MAX_TOKENS = "max_tokens"
        const val KEY_TEMPERATURE = "temperature"
        const val KEY_MODEL = "model"
    }
}
