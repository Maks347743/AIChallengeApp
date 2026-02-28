package com.example.aichallengeapp.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichallengeapp.feature.settings.domain.model.ChatSettings
import com.example.aichallengeapp.feature.settings.domain.repository.ChatSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val chatId: String,
    private val settingsRepository: ChatSettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(settings = settingsRepository.load(chatId)) }
        }
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.UpdateModel -> updateSettings { copy(model = intent.model) }
            is SettingsIntent.UpdateSystemPrompt -> updateSettings { copy(systemPrompt = intent.text) }
            is SettingsIntent.UpdateMaxTokens -> updateSettings { copy(maxTokensText = intent.value) }
            is SettingsIntent.UpdateTemperature -> updateSettings { copy(temperature = intent.value) }
            is SettingsIntent.ToggleSummary -> updateSettings {
                if (intent.enabled) copy(summaryEnabled = true, slidingWindowEnabled = false)
                else copy(summaryEnabled = false)
            }
            is SettingsIntent.UpdateSummaryRecentMessages ->
                intent.value.toIntOrNull()?.let { v -> updateSettings { copy(maxRecentMessages = v) } }
            is SettingsIntent.UpdateSummaryMaxTokens ->
                intent.value.toIntOrNull()?.let { v -> updateSettings { copy(summaryMaxTokens = v) } }
            is SettingsIntent.ToggleSlidingWindow -> updateSettings {
                if (intent.enabled) copy(slidingWindowEnabled = true, summaryEnabled = false)
                else copy(slidingWindowEnabled = false)
            }
            is SettingsIntent.UpdateSlidingWindowSize ->
                intent.value.toIntOrNull()?.let { v -> updateSettings { copy(slidingWindowSize = v) } }
        }
    }

    private fun updateSettings(block: ChatSettings.() -> ChatSettings) {
        val newSettings = _state.value.settings.block()
        _state.update { it.copy(settings = newSettings) }
        viewModelScope.launch {
            settingsRepository.save(chatId, newSettings)
        }
    }
}
