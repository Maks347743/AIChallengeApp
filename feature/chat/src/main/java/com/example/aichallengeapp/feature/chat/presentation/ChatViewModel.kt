package com.example.aichallengeapp.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.ChatSession
import com.example.aichallengeapp.core.database.domain.repository.ChatSessionRepository
import com.example.aichallengeapp.feature.chat.domain.usecase.SendChatMessageUseCase
import com.example.aichallengeapp.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatId: String,
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: ChatSessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val session = sessionRepository.getSession(chatId)
            if (session != null) {
                _state.update { it.copy(messages = session.messages) }
            }
        }
    }

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SendMessage -> sendMessage()
            is ChatIntent.UpdateInput -> _state.update { it.copy(inputText = intent.text) }
            is ChatIntent.ClearChat -> {
                _state.update {
                    it.copy(messages = emptyList(), error = null, lastMetrics = null, showMetrics = false)
                }
                persistSession(emptyList())
            }
            is ChatIntent.ToggleMetrics -> _state.update { it.copy(showMetrics = !it.showMetrics) }
        }
    }

    private fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isEmpty() || _state.value.isLoading) return

        val settings = settingsRepository.load()
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
            val fullHistory = buildList {
                add(ChatMessage(role = "system", content = settings.systemPrompt))
                addAll(_state.value.messages)
            }

            sendChatMessageUseCase(fullHistory, settings.maxTokens, settings.temperature, settings.model.id)
                .onSuccess { result ->
                    val assistantMessage = ChatMessage(
                        role = ChatMessage.ROLE_ASSISTANT,
                        content = result.message
                    )
                    _state.update {
                        it.copy(
                            messages = it.messages + assistantMessage,
                            isLoading = false,
                            lastMetrics = result.metrics
                        )
                    }
                    persistSession(_state.value.messages)
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

    private fun persistSession(messages: List<ChatMessage>) {
        viewModelScope.launch {
            if (messages.isEmpty()) {
                sessionRepository.deleteSession(chatId)
            } else {
                val existing = sessionRepository.getSession(chatId)
                val session = existing?.copy(messages = messages, updatedAt = System.currentTimeMillis())
                    ?: ChatSession(id = chatId, messages = messages)
                sessionRepository.upsertSession(session)
            }
        }
    }
}
