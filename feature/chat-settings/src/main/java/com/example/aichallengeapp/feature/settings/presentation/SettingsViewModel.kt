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
            val settings = settingsRepository.load(chatId)
            _state.update {
                it.copy(
                    settings = settings,
                    maxTokensText = settings.maxTokens?.toString() ?: "",
                    maxRecentMessagesText = settings.retainedMessageCount.toString(),
                    summaryMaxTokensText = settings.summaryMaxTokens.toString(),
                    slidingWindowSizeText = settings.slidingWindowSize.toString(),
                    stickyFactsRecentMessagesText = settings.stickyFactsRecentMessages.toString(),
                )
            }
        }
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.UpdateModel -> updateSettings { copy(model = intent.model) }
            is SettingsIntent.UpdateSystemPrompt -> updateSettings { copy(systemPrompt = intent.text) }
            is SettingsIntent.UpdateMaxTokens -> {
                _state.update { it.copy(maxTokensText = intent.value) }
                val parsed = intent.value.toIntOrNull()
                updateSettings { copy(maxTokens = parsed) }
            }
            is SettingsIntent.UpdateTemperature -> updateSettings { copy(temperature = intent.value) }
            is SettingsIntent.ToggleSummary -> updateSettings {
                if (intent.enabled) copy(summaryEnabled = true, slidingWindowEnabled = false, stickyFactsEnabled = false)
                else copy(summaryEnabled = false)
            }
            is SettingsIntent.UpdateSummaryRecentMessages -> {
                _state.update { it.copy(maxRecentMessagesText = intent.value) }
                intent.value.toIntOrNull()?.takeIf { it > 0 }
                    ?.let { v -> updateSettings { copy(retainedMessageCount = v) } }
            }
            is SettingsIntent.UpdateSummaryMaxTokens -> {
                _state.update { it.copy(summaryMaxTokensText = intent.value) }
                intent.value.toIntOrNull()?.takeIf { it > 0 }
                    ?.let { v -> updateSettings { copy(summaryMaxTokens = v) } }
            }
            is SettingsIntent.ToggleSlidingWindow -> updateSettings {
                if (intent.enabled) copy(slidingWindowEnabled = true, summaryEnabled = false, stickyFactsEnabled = false)
                else copy(slidingWindowEnabled = false)
            }
            is SettingsIntent.UpdateSlidingWindowSize -> {
                _state.update { it.copy(slidingWindowSizeText = intent.value) }
                intent.value.toIntOrNull()?.takeIf { it > 0 }
                    ?.let { v -> updateSettings { copy(slidingWindowSize = v) } }
            }
            is SettingsIntent.ToggleStickyFacts -> updateSettings {
                if (intent.enabled) copy(stickyFactsEnabled = true, summaryEnabled = false, slidingWindowEnabled = false)
                else copy(stickyFactsEnabled = false)
            }
            is SettingsIntent.UpdateStickyFactsRecentMessages -> {
                _state.update { it.copy(stickyFactsRecentMessagesText = intent.value) }
                intent.value.toIntOrNull()?.takeIf { it > 0 }
                    ?.let { v -> updateSettings { copy(stickyFactsRecentMessages = v) } }
            }
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
