package com.example.aichallengeapp.data

import android.content.SharedPreferences
import com.example.aichallengeapp.presentation.ChatSettings
import com.example.aichallengeapp.presentation.ResponseFormat

class SettingsStorage(private val prefs: SharedPreferences) {

    fun load(): ChatSettings {
        return ChatSettings(
            stopWord = prefs.getString(KEY_STOP_WORD, null) ?: ChatSettings().stopWord,
            maxTokensText = prefs.getString(KEY_MAX_TOKENS, null) ?: "",
            responseFormat = prefs.getString(KEY_RESPONSE_FORMAT, null)
                ?.let { name -> ResponseFormat.entries.find { it.name == name } }
                ?: ResponseFormat.PLAIN_TEXT
        )
    }

    fun save(settings: ChatSettings) {
        prefs.edit()
            .putString(KEY_STOP_WORD, settings.stopWord)
            .putString(KEY_MAX_TOKENS, settings.maxTokensText)
            .putString(KEY_RESPONSE_FORMAT, settings.responseFormat.name)
            .apply()
    }

    private companion object {
        const val KEY_STOP_WORD = "stop_word"
        const val KEY_MAX_TOKENS = "max_tokens"
        const val KEY_RESPONSE_FORMAT = "response_format"
    }
}
