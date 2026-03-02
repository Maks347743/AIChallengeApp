package com.example.aichallengeapp.feature.globalsettings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichallengeapp.feature.globalsettings.domain.model.GlobalSettings
import com.example.aichallengeapp.feature.globalsettings.domain.repository.GlobalSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GlobalSettingsViewModel(
    private val repository: GlobalSettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GlobalSettingsState())
    val state: StateFlow<GlobalSettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = repository.load()
            _state.update { it.copy(systemPromptPrefix = settings.systemPromptPrefix) }
        }
    }

    fun onIntent(intent: GlobalSettingsIntent) {
        when (intent) {
            is GlobalSettingsIntent.UpdatePrefix -> {
                _state.update { it.copy(systemPromptPrefix = intent.text) }
                viewModelScope.launch {
                    repository.save(GlobalSettings(systemPromptPrefix = intent.text))
                }
            }
        }
    }
}
