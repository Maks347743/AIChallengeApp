package com.example.aichallengeapp.feature.chat.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.ChatMetrics
import com.example.aichallengeapp.core.database.domain.model.ChatSession
import com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository
import com.example.aichallengeapp.core.database.domain.repository.ChatSessionRepository
import com.example.aichallengeapp.feature.chat.domain.usecase.SendChatMessageUseCase
import com.example.aichallengeapp.feature.globalsettings.domain.repository.GlobalSettingsRepository
import com.example.aichallengeapp.feature.settings.domain.model.ChatSettings
import com.example.aichallengeapp.feature.settings.domain.repository.ChatSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_BRANCHES = 2
private const val FIRST_BRANCH_INDEX = 1
private const val MEMORY_LOG_TAG = "MemoryLayers"
private const val MSG_PREVIEW_LEN = 120

private const val ROLE_LABEL_ASSISTANT = "Ассистент"
private const val ROLE_LABEL_USER = "Пользователь"
private const val SUMMARIZER_SYSTEM_PROMPT = "Ты краткий суммаризатор переписок."
private const val FACTS_EXTRACTOR_SYSTEM_PROMPT =
    "Ты краткий суммаризатор переписок, который может вычленять важные данные из переписки"
private const val TASK_DETECTOR_SYSTEM_PROMPT =
    "Ты отлично понимаешь, когда меняется задача или когда начинается новая задача. " +
    "Если в диалоге меньше 3 сообщений, то это всегда новая задача. " +
    "Проанализируй следующие сообщения. Проверь это новая задача или нет. " +
    "Если задача новая, то в ответ пришли основные факты по задаче, цель, ограничения (если есть) и т.д. " +
    "в формате ключ:значение. Только ключ:значение и ничего больше. Очень кратко. " +
    "Если задача не новая — ответь только словом NO_CHANGE."

class ChatViewModel(
    private val chatId: String,
    private val initialBranchIndex: Int,
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val settingsRepository: ChatSettingsRepository,
    private val sessionRepository: ChatSessionRepository,
    private val metricsRepository: ChatMetricsRepository,
    private val globalSettingsRepository: GlobalSettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _activeChatId = MutableStateFlow(chatId)
    private val activeChatId: String get() = _activeChatId.value

    private var currentGroupId: String? = null
    private var currentTask: String? = null

    init {
        viewModelScope.launch {
            val session = sessionRepository.getSession(chatId)
            if (session != null) {
                val groupId = session.checkpointGroupId
                if (groupId != null && session.branchIndex != initialBranchIndex) {
                    // Defensive: ViewModel reuse — find the session matching the requested branch
                    val target = sessionRepository.getSessionsByGroup(groupId)
                        .firstOrNull { it.branchIndex == initialBranchIndex }
                    if (target != null) {
                        _activeChatId.value = target.id
                        _state.update { it.copy(messages = target.messages, activeChatId = target.id) }
                        currentTask = target.currentTask
                        loadBranches(groupId, initialBranchIndex)
                        return@launch
                    }
                }
                _state.update { it.copy(messages = session.messages, activeChatId = chatId) }
                currentTask = session.currentTask
                if (groupId != null) loadBranches(groupId, session.branchIndex)
            } else {
                _state.update { it.copy(activeChatId = chatId) }
            }
        }
        viewModelScope.launch {
            _activeChatId
                .flatMapLatest { id -> metricsRepository.observeMetrics(id) }
                .collect { metrics -> _state.update { it.copy(chatMetrics = metrics) } }
        }
    }

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SendMessage -> sendMessage()
            is ChatIntent.UpdateInput -> _state.update { it.copy(inputText = intent.text) }
            is ChatIntent.ClearChat -> clearChat()
            is ChatIntent.ToggleMetrics -> _state.update { it.copy(showMetrics = !it.showMetrics) }
            is ChatIntent.CreateCheckpoint -> createCheckpoint()
            is ChatIntent.SwitchBranch -> switchBranch(intent.sessionId)
        }
    }

    fun onNavigatingBack(navigate: () -> Unit) {
        val groupId = currentGroupId
        if (groupId == null) {
            navigate()
            return
        }
        viewModelScope.launch {
            cleanupGroupOnExit(groupId)
            navigate()
        }
    }

    private fun switchBranch(sessionId: String) {
        if (sessionId == activeChatId || _state.value.isLoading) return
        viewModelScope.launch {
            val session = sessionRepository.getSession(sessionId) ?: return@launch
            _activeChatId.value = sessionId
            currentTask = session.currentTask
            _state.update {
                it.copy(
                    messages = session.messages,
                    activeBranchIndex = session.branchIndex,
                    activeChatId = sessionId,
                    inputText = "",
                    error = null,
                    chatMetrics = null
                )
            }
        }
    }

    private suspend fun cleanupGroupOnExit(groupId: String) {
        // Explicitly delete current session if empty — avoids race with clearChat's async delete
        if (_state.value.messages.isEmpty()) {
            sessionRepository.deleteSession(activeChatId)
        }
        val sessions = sessionRepository.getSessionsByGroup(groupId)
        sessions.filter { it.messages.isEmpty() }.forEach { sessionRepository.deleteSession(it.id) }
        val remaining = sessionRepository.getSessionsByGroup(groupId)
        if (remaining.size == 1) {
            val sole = remaining.first()
            sessionRepository.upsertSession(sole.copy(checkpointGroupId = null, branchIndex = 0))
        }
    }

    private suspend fun loadBranches(groupId: String, activeBranchIndex: Int) {
        currentGroupId = groupId
        val siblings = sessionRepository.getSessionsByGroup(groupId)
        if (siblings.size <= 1) {
            // Sole survivor — sibling was deleted but cleanup didn't run; heal the DB now
            sessionRepository.getSession(activeChatId)?.let { session ->
                sessionRepository.upsertSession(session.copy(checkpointGroupId = null, branchIndex = 0))
            }
            currentGroupId = null
            return
        }
        _state.update {
            it.copy(
                branches = siblings.map { s -> BranchInfo(s.id, s.branchIndex) },
                activeBranchIndex = activeBranchIndex
            )
        }
    }

    private fun createCheckpoint() {
        if (_state.value.isLoading) return
        if (_state.value.branches.size >= MAX_BRANCHES) return
        viewModelScope.launch {
            val current = sessionRepository.getSession(activeChatId) ?: return@launch
            val groupId: String
            val currentBranchIndex: Int
            val nextBranchIndex: Int

            if (current.checkpointGroupId == null) {
                groupId = java.util.UUID.randomUUID().toString()
                currentBranchIndex = FIRST_BRANCH_INDEX
                nextBranchIndex = FIRST_BRANCH_INDEX + 1
                sessionRepository.updateCheckpointFields(activeChatId, groupId, currentBranchIndex)
            } else {
                groupId = current.checkpointGroupId!!
                val siblings = sessionRepository.getSessionsByGroup(groupId)
                if (siblings.size >= MAX_BRANCHES) return@launch
                currentBranchIndex = current.branchIndex
                nextBranchIndex = siblings.maxOf { it.branchIndex } + 1
            }

            val now = System.currentTimeMillis()
            sessionRepository.upsertSession(
                ChatSession(
                    id = java.util.UUID.randomUUID().toString(),
                    messages = current.messages,
                    createdAt = now,
                    updatedAt = now,
                    settingsJson = current.settingsJson,
                    checkpointGroupId = groupId,
                    branchIndex = nextBranchIndex
                )
            )
            loadBranches(groupId, currentBranchIndex)
        }
    }

    private fun clearChat() {
        currentTask = null
        _state.update { it.copy(showMetrics = false, messages = emptyList(), error = null) }
        viewModelScope.launch {
            metricsRepository.deleteMetrics(activeChatId)
            persistSession(emptyList())
            // Proactively un-branch the sibling so the DB is consistent before the user navigates
            val groupId = currentGroupId
            if (groupId != null) {
                val remaining = sessionRepository.getSessionsByGroup(groupId)
                if (remaining.size == 1) {
                    val sole = remaining.first()
                    sessionRepository.upsertSession(sole.copy(checkpointGroupId = null, branchIndex = 0))
                    _activeChatId.value = sole.id
                    _state.update {
                        it.copy(
                            messages = sole.messages,
                            activeChatId = sole.id,
                            branches = emptyList(),
                            activeBranchIndex = 0
                        )
                    }
                } else {
                    _state.update { it.copy(branches = emptyList(), activeBranchIndex = 0) }
                }
                currentGroupId = null
            }
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
            val settings = settingsRepository.load(activeChatId)
            val globalSettings = globalSettingsRepository.load()
            val globalPrefix = globalSettings.systemPromptPrefix

            val detectedTask = detectCurrentTask(existingMessages, userMessage, settings.model.id)
            if (detectedTask != null) {
                currentTask = detectedTask
            }

            logMemoryLayers(globalPrefix, existingMessages, userMessage)

            val doStickyFacts = settings.stickyFactsEnabled
                && existingMessages.size > settings.stickyFactsRecentMessages
            val doSummary = settings.summaryEnabled
                && existingMessages.size > settings.retainedMessageCount
            if (doStickyFacts) {
                sendMessageWithStickyFacts(settings, globalPrefix, existingMessages, userMessage)
            } else if (doSummary) {
                sendMessageWithSummary(settings, globalPrefix, existingMessages, userMessage)
            } else {
                sendMessageNormal(settings, globalPrefix)
            }
        }
    }

    private fun logMemoryLayers(
        globalPrefix: String,
        history: List<ChatMessage>,
        newUserMessage: ChatMessage
    ) {
        val sep = "─".repeat(52)
        Log.d(MEMORY_LOG_TAG, "┌$sep")
        Log.d(MEMORY_LOG_TAG, "│  MEMORY LAYERS — before system prompt build")
        Log.d(MEMORY_LOG_TAG, "├─── [1] LONG_TERM_MEMORY (global prefix) ${"─".repeat(10)}")
        if (globalPrefix.isBlank()) {
            Log.d(MEMORY_LOG_TAG, "│  <empty>")
        } else {
            globalPrefix.lines().forEach { Log.d(MEMORY_LOG_TAG, "│  $it") }
        }
        Log.d(MEMORY_LOG_TAG, "├─── [2] SHORT_TERM_MEMORY (${history.size} messages) ${"─".repeat(15)}")
        history.forEach { msg ->
            val label = when (msg.role) {
                ChatMessage.ROLE_USER      -> "USER"
                ChatMessage.ROLE_ASSISTANT -> "ASST"
                ChatMessage.ROLE_SYSTEM    -> "SYS "
                ChatMessage.ROLE_SUMMARY   -> "SUMM"
                ChatMessage.ROLE_FACTS     -> "FACT"
                else                       -> msg.role.take(4).uppercase()
            }
            val preview = msg.content.replace('\n', ' ').take(MSG_PREVIEW_LEN)
            val ellipsis = if (msg.content.length > MSG_PREVIEW_LEN) "…" else ""
            Log.d(MEMORY_LOG_TAG, "│  [$label] $preview$ellipsis")
        }
        val newPreview = newUserMessage.content.replace('\n', ' ').take(MSG_PREVIEW_LEN)
        val newEllipsis = if (newUserMessage.content.length > MSG_PREVIEW_LEN) "…" else ""
        Log.d(MEMORY_LOG_TAG, "│  [USER] (new) $newPreview$newEllipsis")
        Log.d(MEMORY_LOG_TAG, "├─── [3] CURRENT_TASK_MEMORY ${"─".repeat(23)}")
        if (currentTask.isNullOrBlank()) {
            Log.d(MEMORY_LOG_TAG, "│  <empty>")
        } else {
            currentTask!!.lines().forEach { Log.d(MEMORY_LOG_TAG, "│  $it") }
        }
        Log.d(MEMORY_LOG_TAG, "└$sep")
    }

    private fun effectiveSystemPrompt(globalPrefix: String, chatPrompt: String): String {
        val base = if (globalPrefix.isBlank()) chatPrompt else "$globalPrefix\n\n$chatPrompt"
        return if (currentTask.isNullOrBlank()) base else "$base\n\nТекущая задача:\n$currentTask"
    }

    private suspend fun detectCurrentTask(
        existingMessages: List<ChatMessage>,
        newUserMessage: ChatMessage,
        model: String
    ): String? {
        val contextMessages = existingMessages
            .filter { it.role == ChatMessage.ROLE_USER || it.role == ChatMessage.ROLE_ASSISTANT }
            .takeLast(3)
        val contextText = buildString {
            contextMessages.forEach { msg ->
                val label = if (msg.role == ChatMessage.ROLE_ASSISTANT) ROLE_LABEL_ASSISTANT else ROLE_LABEL_USER
                append("$label: ${msg.content}\n")
            }
            append("$ROLE_LABEL_USER: ${newUserMessage.content}\n")
        }
        val result = sendChatMessageUseCase(
            messages = listOf(
                ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = TASK_DETECTOR_SYSTEM_PROMPT),
                ChatMessage(role = ChatMessage.ROLE_USER, content = contextText)
            ),
            maxTokens = null,
            temperature = null,
            model = model
        )
        return result.getOrNull()?.message?.trim()
            ?.takeIf { it.isNotBlank() && it != "NO_CHANGE" }
    }

    private suspend fun sendMessageNormal(
        settings: ChatSettings,
        globalPrefix: String
    ) {
        val fullHistory = buildList {
            add(ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = effectiveSystemPrompt(globalPrefix, settings.systemPrompt)))
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
                        chatId = activeChatId,
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
        globalPrefix: String,
        existingMessages: List<ChatMessage>,
        userMessage: ChatMessage
    ) {
        val retainedMessageCount = settings.retainedMessageCount
        val olderMessages = existingMessages.dropLast(retainedMessageCount)
        val recentMessages = existingMessages.takeLast(retainedMessageCount)
        val summaryMaxTokens = settings.summaryMaxTokens

        val previousSummary = olderMessages.firstOrNull { it.role == ChatMessage.ROLE_SUMMARY }
        val olderNonSummary = olderMessages.filter { it.role != ChatMessage.ROLE_SUMMARY }
        val conversationText = buildString {
            if (previousSummary != null) {
                append("${previousSummary.content}\n\n")
            }
            append(olderNonSummary.joinToString("\n") { msg ->
                val label = if (msg.role == ChatMessage.ROLE_ASSISTANT) ROLE_LABEL_ASSISTANT else ROLE_LABEL_USER
                "$label: ${msg.content}"
            })
        }
        val summaryHistory = listOf(
            ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = SUMMARIZER_SYSTEM_PROMPT),
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
            add(ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = "${effectiveSystemPrompt(globalPrefix, settings.systemPrompt)}\n\nКонтекст предыдущих сообщений:\n$summaryContent"))
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
                    chatId = activeChatId,
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

    private suspend fun sendMessageWithStickyFacts(
        settings: ChatSettings,
        globalPrefix: String,
        existingMessages: List<ChatMessage>,
        userMessage: ChatMessage
    ) {
        val recentMessages = existingMessages.takeLast(settings.stickyFactsRecentMessages)
        val olderMessages = existingMessages.dropLast(settings.stickyFactsRecentMessages)

        val previousFacts = olderMessages.firstOrNull { it.role == ChatMessage.ROLE_FACTS }
        val olderNonFacts = olderMessages.filter { it.role != ChatMessage.ROLE_FACTS }

        val conversationText = buildString {
            if (previousFacts != null) append("${previousFacts.content}\n\n")
            append(olderNonFacts.joinToString("\n") { msg ->
                val label = if (msg.role == ChatMessage.ROLE_ASSISTANT) ROLE_LABEL_ASSISTANT else ROLE_LABEL_USER
                "$label: ${msg.content}"
            })
        }

        val factsHistory = listOf(
            ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = FACTS_EXTRACTOR_SYSTEM_PROMPT),
            ChatMessage(role = ChatMessage.ROLE_USER, content = "Сделай краткую выдержку важных данных следующей переписки в формате факт:значение. Анализируй текст, который идет после слова Контент. Каждый новый факт переноси на новую строку, чтобы выглядело аккуратно. Контент:\n\n$conversationText")
        )
        val factsResult = sendChatMessageUseCase(factsHistory, maxTokens = null, temperature = null, model = settings.model.id)

        if (factsResult.isFailure) {
            _state.update { it.copy(isLoading = false, error = factsResult.exceptionOrNull()?.message ?: "Facts extraction failed") }
            return
        }

        val factsContent = factsResult.getOrNull()!!.message

        val mainHistory = buildList {
            add(ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = effectiveSystemPrompt(globalPrefix, settings.systemPrompt)))
            add(ChatMessage(role = ChatMessage.ROLE_USER, content = factsContent))
            addAll(recentMessages.filter { it.role != ChatMessage.ROLE_FACTS })
            add(userMessage)
        }
        val mainResult = sendChatMessageUseCase(mainHistory, settings.maxTokens, settings.temperature, settings.model.id)

        mainResult.onSuccess { result ->
            val factsMessage = ChatMessage(role = ChatMessage.ROLE_FACTS, content = factsContent)
            val assistantMessage = ChatMessage(role = ChatMessage.ROLE_ASSISTANT, content = result.message)
            val newMessages = listOf(factsMessage) +
                recentMessages.filter { it.role != ChatMessage.ROLE_FACTS } +
                userMessage + assistantMessage
            _state.update { it.copy(messages = newMessages, isLoading = false) }
            persistSession(newMessages)

            val currentTotal = _state.value.chatMetrics?.totalTokens ?: 0
            val newTotal = currentTotal + result.metrics.promptTokens + result.metrics.completionTokens
            metricsRepository.upsertMetrics(
                ChatMetrics(
                    chatId = activeChatId,
                    lastRequestTokens = result.metrics.promptTokens,
                    lastResponseTokens = result.metrics.completionTokens,
                    totalTokens = newTotal
                )
            )
        }
        mainResult.onFailure { throwable ->
            _state.update { it.copy(isLoading = false, error = throwable.message ?: "Unknown error") }
        }
    }

    private suspend fun persistSession(messages: List<ChatMessage>) {
        if (messages.isEmpty()) {
            sessionRepository.deleteSession(activeChatId)
        } else {
            val existing = sessionRepository.getSession(activeChatId)
            val session = existing?.copy(
                messages = messages,
                updatedAt = System.currentTimeMillis(),
                currentTask = currentTask
            ) ?: ChatSession(id = activeChatId, messages = messages, currentTask = currentTask)
            sessionRepository.upsertSession(session)
        }
    }
}
