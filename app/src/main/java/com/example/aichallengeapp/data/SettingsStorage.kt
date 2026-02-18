package com.example.aichallengeapp.data

import android.content.SharedPreferences
import com.example.aichallengeapp.presentation.ChatSettings

class SettingsStorage(private val prefs: SharedPreferences) {

    fun load(): ChatSettings {
        return ChatSettings(
            systemPrompt = prefs.getString(KEY_SYSTEM_PROMPT, null) ?: ChatSettings().systemPrompt,
            maxTokensText = prefs.getString(KEY_MAX_TOKENS, null) ?: "",
            temperature = prefs.getFloat(KEY_TEMPERATURE, ChatSettings().temperature)
        )
    }

    fun save(settings: ChatSettings) {
        prefs.edit()
            .putString(KEY_SYSTEM_PROMPT, settings.systemPrompt)
            .putString(KEY_MAX_TOKENS, settings.maxTokensText)
            .putFloat(KEY_TEMPERATURE, settings.temperature)
            .apply()
    }

    private companion object {
        const val KEY_SYSTEM_PROMPT = "system_prompt"
        const val KEY_MAX_TOKENS = "max_tokens"
        const val KEY_TEMPERATURE = "temperature"
    }
}
