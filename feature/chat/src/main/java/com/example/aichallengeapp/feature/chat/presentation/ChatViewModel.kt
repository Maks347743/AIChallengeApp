package com.example.aichallengeapp.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.ChatMetrics
import com.example.aichallengeapp.core.database.domain.model.ChatSession
import com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository
import com.example.aichallengeapp.core.database.domain.repository.ChatSessionRepository
import com.example.aichallengeapp.feature.chat.domain.usecase.SendChatMessageUseCase
import com.example.aichallengeapp.feature.settings.domain.model.ChatSettings
import com.example.aichallengeapp.feature.settings.domain.repository.ChatSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatId: String,
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val settingsRepository: ChatSettingsRepository,
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

        val existingMessages = _state.value.messages
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
            val settings = settingsRepository.load(chatId)
            val doSummary = settings.summaryEnabled
                && existingMessages.size > settings.maxRecentMessages
            if (doSummary) {
                sendMessageWithSummary(settings, existingMessages, userMessage)
            } else {
                sendMessageNormal(settings)
            }
        }
    }

    private suspend fun sendMessageNormal(
        settings: ChatSettings
    ) {
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
                val messagesWithResponse = _state.value.messages + assistantMessage
                val finalMessages = if (settings.slidingWindowEnabled
                    && messagesWithResponse.size > settings.slidingWindowSize
                ) {
                    messagesWithResponse.takeLast(settings.slidingWindowSize)
                } else {
                    messagesWithResponse
                }
                _state.update { it.copy(messages = finalMessages, isLoading = false) }
                persistSession(finalMessages)

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

    private suspend fun sendMessageWithSummary(
        settings: ChatSettings,
        existingMessages: List<ChatMessage>,
        userMessage: ChatMessage
    ) {
        val maxRecentMessages = settings.maxRecentMessages
        val olderMessages = existingMessages.dropLast(maxRecentMessages)
        val recentMessages = existingMessages.takeLast(maxRecentMessages)
        val summaryMaxTokens = settings.summaryMaxTokens

        val previousSummary = olderMessages.firstOrNull { it.role == ChatMessage.ROLE_SUMMARY }
        val olderNonSummary = olderMessages.filter { it.role != ChatMessage.ROLE_SUMMARY }
        val conversationText = buildString {
            if (previousSummary != null) {
                append("${previousSummary.content}\n\n")
            }
            append(olderNonSummary.joinToString("\n") { msg ->
                val label = if (msg.role == ChatMessage.ROLE_ASSISTANT) "Ассистент" else "Пользователь"
                "$label: ${msg.content}"
            })
        }
        val summaryHistory = listOf(
            ChatMessage(role = "system", content = "Ты краткий суммаризатор переписок."),
            ChatMessage(
                role = ChatMessage.ROLE_USER,
                content = "Сделай краткое summary следующей переписки. Уложись в $summaryMaxTokens токенов:\n\n$conversationText"
            )
        )
        val summaryResult = sendChatMessageUseCase(
            summaryHistory,
            temperature = null,
            maxTokens = null,
            model = settings.model.id
        )

        if (summaryResult.isFailure) {
            _state.update {
                it.copy(
                    isLoading = false,
                    error = summaryResult.exceptionOrNull()?.message ?: "Summary request failed"
                )
            }
            return
        }

        val summaryContent = summaryResult.getOrNull()!!.message

        val mainHistory = buildList {
            add(ChatMessage(role = "system", content = "${settings.systemPrompt}\n\nКонтекст предыдущих сообщений:\n$summaryContent"))
            addAll(recentMessages.filter { it.role != ChatMessage.ROLE_SUMMARY })
            add(userMessage)
        }
        val mainResult = sendChatMessageUseCase(
            mainHistory,
            settings.maxTokens,
            settings.temperature,
            settings.model.id
        )

        mainResult.onSuccess { result ->
            val summaryMessage = ChatMessage(role = ChatMessage.ROLE_SUMMARY, content = summaryContent)
            val assistantMessage = ChatMessage(role = ChatMessage.ROLE_ASSISTANT, content = result.message)
            val newMessages = listOf(summaryMessage) + recentMessages + userMessage + assistantMessage
            _state.update { it.copy(messages = newMessages, isLoading = false) }
            persistSession(newMessages)

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
        mainResult.onFailure { throwable ->
            _state.update {
                it.copy(
                    isLoading = false,
                    error = throwable.message ?: "Unknown error"
                )
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
