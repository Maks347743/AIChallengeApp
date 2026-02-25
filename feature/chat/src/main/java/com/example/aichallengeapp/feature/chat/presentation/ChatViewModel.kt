package com.example.aichallengeapp.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.ChatMetrics
import com.example.aichallengeapp.core.database.domain.model.ChatSession
import com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository
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
    private val sessionRepository: ChatSessionRepository,
    private val metricsRepository: ChatMetricsRepository
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
        viewModelScope.launch {
            metricsRepository.observeMetrics(chatId).collect { metrics ->
                _state.update { it.copy(chatMetrics = metrics) }
            }
        }
    }

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SendMessage -> sendMessage()
            is ChatIntent.UpdateInput -> _state.update { it.copy(inputText = intent.text) }
            is ChatIntent.ClearChat -> clearChat()
            is ChatIntent.ToggleMetrics -> _state.update { it.copy(showMetrics = !it.showMetrics) }
        }
    }

    private fun clearChat() {
        _state.update { it.copy(showMetrics = false, messages = emptyList(), error = null) }
        viewModelScope.launch {
            metricsRepository.deleteMetrics(chatId)
            persistSession(emptyList())
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
                            isLoading = false
                        )
                    }
                    persistSession(_state.value.messages)  // awaited: session exists before metrics insert

                    val currentTotal = _state.value.chatMetrics?.totalTokens ?: 0
                    val newTotal = currentTotal + result.metrics.promptTokens + result.metrics.completionTokens
                    metricsRepository.upsertMetrics(
                        ChatMetrics(
                            chatId = chatId,
                            lastRequestTokens = result.metrics.promptTokens,
                            lastResponseTokens = result.metrics.completionTokens,
                            totalTokens = newTotal
                        )
                    )
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

    private suspend fun persistSession(messages: List<ChatMessage>) {
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
