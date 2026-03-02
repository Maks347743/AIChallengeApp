package com.example.aichallengeapp.feature.globalsettings.data

import android.content.Context
import com.example.aichallengeapp.feature.globalsettings.domain.model.GlobalSettings
import com.example.aichallengeapp.feature.globalsettings.domain.repository.GlobalSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PREFS_NAME = "global_settings"
private const val KEY_SETTINGS = "settings"

class GlobalSettingsRepositoryImpl(private val context: Context) : GlobalSettingsRepository {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun load(): GlobalSettings = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_SETTINGS, null) ?: return@withContext GlobalSettings()
        runCatching { Json.decodeFromString<GlobalSettings>(json) }.getOrDefault(GlobalSettings())
    }

    override suspend fun save(settings: GlobalSettings) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_SETTINGS, Json.encodeToString(settings)).apply()
    }
}
