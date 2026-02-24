package com.example.aichallengeapp.presentation

import androidx.lifecycle.ViewModel
import com.example.aichallengeapp.data.SettingsStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsState(
    val settings: ChatSettings = ChatSettings()
)

sealed interface SettingsIntent {
    data class UpdateMaxTokens(val value: String) : SettingsIntent
    data class UpdateSystemPrompt(val text: String) : SettingsIntent
    data class UpdateTemperature(val value: Float) : SettingsIntent
    data class UpdateModel(val model: DeepSeekModel) : SettingsIntent
}

class SettingsViewModel(private val settingsStorage: SettingsStorage) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState(settings = settingsStorage.load()))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.UpdateModel -> updateSettings { copy(model = intent.model) }
            is SettingsIntent.UpdateSystemPrompt -> updateSettings { copy(systemPrompt = intent.text) }
            is SettingsIntent.UpdateMaxTokens -> updateSettings { copy(maxTokensText = intent.value) }
            is SettingsIntent.UpdateTemperature -> updateSettings { copy(temperature = intent.value) }
        }
    }

    private fun updateSettings(block: ChatSettings.() -> ChatSettings) {
        _state.update {
            val newSettings = it.settings.block()
            settingsStorage.save(newSettings)
            it.copy(settings = newSettings)
        }
    }
}
