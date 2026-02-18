package com.example.aichallengeapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichallengeapp.domain.model.ChatMessage
import com.example.aichallengeapp.data.SettingsStorage
import com.example.aichallengeapp.domain.usecase.SendChatMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val settingsStorage: SettingsStorage
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState(settings = settingsStorage.load()))
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private fun updateSettings(block: ChatSettings.() -> ChatSettings) {
        _state.update {
            val newSettings = it.settings.block()
            settingsStorage.save(newSettings)
            it.copy(settings = newSettings)
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SendMessage -> sendMessage()
            is HomeIntent.UpdateInput -> _state.update { it.copy(inputText = intent.text) }
            is HomeIntent.ClearChat -> _state.update {
                it.copy(messages = emptyList(), error = null)
            }
            is HomeIntent.UpdateMaxTokens -> updateSettings { copy(maxTokensText = intent.value) }
            is HomeIntent.UpdateSystemPrompt -> updateSettings { copy(systemPrompt = intent.text) }
            is HomeIntent.UpdateTemperature -> updateSettings { copy(temperature = intent.value) }
        }
    }

    private fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isEmpty() || _state.value.isLoading) return

        val userMessage = ChatMessage(role = ChatMessage.ROLE_USER, content = text)

        _state.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            val settings = _state.value.settings
            val fullHistory = buildList {
                add(ChatMessage(role = "system", content = settings.systemPrompt))
                addAll(_state.value.messages)
            }

            sendChatMessageUseCase(fullHistory, settings.maxTokens, settings.temperature)
                .onSuccess { responseText ->
                    val assistantMessage = ChatMessage(
                        role = ChatMessage.ROLE_ASSISTANT,
                        content = responseText
                    )
                    _state.update {
                        it.copy(
                            messages = it.messages + assistantMessage,
                            isLoading = false
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Unknown error"
                        )
                    }
                }
        }
    }
}
