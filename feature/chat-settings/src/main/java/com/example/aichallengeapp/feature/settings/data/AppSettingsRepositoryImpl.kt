package com.example.aichallengeapp.feature.settings.data

import android.content.Context
import com.example.aichallengeapp.feature.settings.domain.model.AppSettings
import com.example.aichallengeapp.feature.settings.domain.repository.AppSettingsRepository
import kotlinx.serialization.json.Json
import androidx.core.content.edit

private const val PREFS_NAME = "app_settings"
private const val KEY_APP_SETTINGS = "app_settings_json"

class AppSettingsRepositoryImpl(
    private val context: Context,
    private val json: Json
) : AppSettingsRepository {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun load(): AppSettings {
        val raw = prefs.getString(KEY_APP_SETTINGS, null) ?: return AppSettings()
        return try {
            json.decodeFromString<AppSettings>(raw)
        } catch (_: Exception) {
            AppSettings()
        }
    }

    override suspend fun save(settings: AppSettings) {
        val encoded = json.encodeToString(settings)
        prefs.edit { putString(KEY_APP_SETTINGS, encoded) }
    }
}
