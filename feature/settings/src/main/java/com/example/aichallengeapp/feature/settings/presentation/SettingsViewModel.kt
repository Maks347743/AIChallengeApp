package com.example.aichallengeapp.feature.settings.presentation

import androidx.lifecycle.ViewModel
import com.example.aichallengeapp.feature.settings.domain.model.ChatSettings
import com.example.aichallengeapp.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState(settings = settingsRepository.load()))
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
            settingsRepository.save(newSettings)
            it.copy(settings = newSettings)
        }
    }
}
