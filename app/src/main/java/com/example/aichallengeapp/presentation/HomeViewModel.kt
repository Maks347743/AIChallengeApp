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

private const val SYSTEM_PROMPT_BASE = """
|Ты - бартендер, профессионал, который занимается созданием новых рецептов и приготовлением алкогольных коктейлей. Ты должен помочь мне создать новый напиток, задавая уточняющие вопросы по моим вкусовым предпочтениям.
|Важные правила:
|1. Задавай только один вопрос за раз
|2. Не задавай больше вопросов после стопслова "%s"."""

private const val SYSTEM_PROMPT_PLAIN_TEXT_SUFFIX = """
|3. Если пользователь в ответ напишет стопслово, то ты должен прекратить дальнейший диалог, ответив последней фразой: "Диалог завершен по вашему запросу"
|4. Отвечай обычным текстом."""

private const val SYSTEM_PROMPT_JSON_SUFFIX = """
|3. Отвечай ТОЛЬКО в формате Json, без каких-либо пояснений или текста вокруг.
|4. Json должен быть отформатирован корректно и содержать поля previous_user_message (мое предыдущее сообщение), next_ai_message (твое новое сообщение).
|5. При получении стопслова ответь Json: {"previous_user_message": "<стопслово>", "next_ai_message": "Диалог завершен по вашему запросу"}"""

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
            is HomeIntent.ToggleSettings -> _state.update { it.copy(showSettings = !it.showSettings) }
            is HomeIntent.UpdateStopWord -> updateSettings { copy(stopWord = intent.word) }
            is HomeIntent.UpdateMaxTokens -> updateSettings { copy(maxTokensText = intent.value) }
            is HomeIntent.UpdateResponseFormat -> updateSettings { copy(responseFormat = intent.format) }
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
            val formatSuffix = when (settings.responseFormat) {
                ResponseFormat.PLAIN_TEXT -> SYSTEM_PROMPT_PLAIN_TEXT_SUFFIX
                ResponseFormat.JSON -> SYSTEM_PROMPT_JSON_SUFFIX
            }
            val systemPrompt = (SYSTEM_PROMPT_BASE.format(settings.stopWord) + formatSuffix)
                .trimIndent()
            val fullHistory = buildList {
                add(ChatMessage(role = "system", content = systemPrompt))
                addAll(_state.value.messages)
            }

            sendChatMessageUseCase(fullHistory, settings.maxTokens)
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
