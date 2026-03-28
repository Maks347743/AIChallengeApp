package com.example.aichallengeapp.feature.settings.presentation.appsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichallengeapp.feature.settings.domain.model.AppSettings
import com.example.aichallengeapp.feature.settings.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppSettingsViewModel(
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AppSettingsState())
    val state: StateFlow<AppSettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = appSettingsRepository.load()
            _state.update { it.copy(settings = settings) }
        }
    }

    fun onIntent(intent: AppSettingsIntent) {
        when (intent) {
            is AppSettingsIntent.UpdateModel -> updateSettings { copy(model = intent.model) }
            is AppSettingsIntent.UpdateOllamaModelName -> updateSettings { copy(ollamaModelName = intent.value) }
            is AppSettingsIntent.UpdateServerBaseUrl -> updateSettings { copy(serverBaseUrl = intent.value) }
            is AppSettingsIntent.UpdateMcpServerToken -> updateSettings { copy(mcpServerToken = intent.value) }
        }
    }

    private fun updateSettings(block: AppSettings.() -> AppSettings) {
        val newSettings = _state.value.settings.block()
        _state.update { it.copy(settings = newSettings) }
        viewModelScope.launch {
            appSettingsRepository.save(newSettings)
        }
    }
}
