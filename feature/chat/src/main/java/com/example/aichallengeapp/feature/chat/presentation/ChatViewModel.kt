package com.example.aichallengeapp.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.ChatResult
import com.example.aichallengeapp.core.database.domain.model.Constraint
import com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository
import com.example.aichallengeapp.core.database.domain.repository.UserProfileRepository
import com.example.aichallengeapp.core.mcp.model.ToolDefinition
import com.example.aichallengeapp.core.periodictask.domain.model.PeriodicTaskMessageBus
import kotlinx.serialization.json.Json
import com.example.aichallengeapp.feature.chat.domain.ChatSessionManager
import com.example.aichallengeapp.feature.chat.domain.PromptTemplates
import com.example.aichallengeapp.feature.chat.domain.usecase.BuildSystemPromptUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.ExecuteToolCallsUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.GetToolDefinitionsUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.SendChatMessageUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.UpdateMetricsUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.ValidateConstraintsUseCase
import com.example.aichallengeapp.feature.settings.domain.model.ChatSettings
import com.example.aichallengeapp.feature.settings.domain.repository.ChatSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

private const val MAX_CONSTRAINT_RETRIES = 3
private const val MAX_TOOL_ITERATIONS = 5

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val chatId: String,
    private val initialBranchIndex: Int,
    private val initialProfileId: String?,
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val validateConstraintsUseCase: ValidateConstraintsUseCase,
    private val buildSystemPromptUseCase: BuildSystemPromptUseCase,
    private val updateMetricsUseCase: UpdateMetricsUseCase,
    private val executeToolCallsUseCase: ExecuteToolCallsUseCase,
    private val getToolDefinitionsUseCase: GetToolDefinitionsUseCase,
    private val settingsRepository: ChatSettingsRepository,
    private val sessionManager: ChatSessionManager,
    private val metricsRepository: ChatMetricsRepository,
    private val userProfileRepository: UserProfileRepository,
    private val periodicTaskMessageBus: PeriodicTaskMessageBus
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _activeChatId = MutableStateFlow(chatId)
    private val activeChatId: String get() = _activeChatId.value

    private var cachedToolDefinitions: List<ToolDefinition>? = null

    init {
        viewModelScope.launch {
            val result = sessionManager.loadSession(chatId, initialBranchIndex)
            val target = result.targetSession
            val session = result.session

            if (target != null) {
                _activeChatId.value = target.id
                _state.update {
                    it.copy(
                        messages = target.messages,
                        activeChatId = target.id,
                        currentProfileId = target.profileId ?: initialProfileId,
                        isPeriodicTask = target.isPeriodicTask,
                        branches = result.branches,
                        activeBranchIndex = result.activeBranchIndex
                    )
                }
            } else if (session != null) {
                _state.update {
                    it.copy(
                        messages = session.messages,
                        activeChatId = chatId,
                        currentProfileId = session.profileId ?: initialProfileId,
                        isPeriodicTask = session.isPeriodicTask,
                        branches = result.branches,
                        activeBranchIndex = result.activeBranchIndex
                    )
                }
            } else {
                _state.update { it.copy(activeChatId = chatId, currentProfileId = initialProfileId) }
            }
            loadProfileName()
        }
        viewModelScope.launch {
            _activeChatId
                .flatMapLatest { id -> metricsRepository.observeMetrics(id) }
                .collect { metrics -> _state.update { it.copy(chatMetrics = metrics) } }
        }
        viewModelScope.launch { loadToolDefinitions() }
        viewModelScope.launch {
            periodicTaskMessageBus.messages.collect { message ->
                if (message.chatId == activeChatId) {
                    val periodicMessage = ChatMessage(
                        role = ChatMessage.ROLE_ASSISTANT,
                        content = "\uD83D\uDD04 **Periodic task** (${message.toolName}):\n${message.summary}"
                    )
                    _state.update { it.copy(messages = it.messages + periodicMessage) }
                    persistSession(_state.value.messages)
                }
            }
        }
    }

    private suspend fun loadToolDefinitions() {
        try {
            val result = getToolDefinitionsUseCase()
            cachedToolDefinitions = result.all
            Timber.tag("ChatViewModel").d("Loaded ${result.all.size} tool definitions (${result.mcpCount} MCP)")
            _state.update { it.copy(mcpToolsCount = result.mcpCount) }
        } catch (e: Exception) {
            Timber.tag("ChatViewModel").w(e, "MCP server unavailable, continuing without tools")
            _state.update { it.copy(mcpToolsCount = 0) }
        }
    }

    private suspend fun loadProfileName() {
        val profile = _state.value.currentProfileId?.let { userProfileRepository.getById(it) }
        _state.update { it.copy(currentProfileName = profile?.name) }
    }

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SendMessage -> sendMessage()
            is ChatIntent.UpdateInput -> _state.update { it.copy(inputText = intent.text) }
            is ChatIntent.ClearChat -> clearChat()
            is ChatIntent.ToggleMetrics -> _state.update { it.copy(showMetrics = !it.showMetrics) }
            is ChatIntent.CreateCheckpoint -> createCheckpoint()
            is ChatIntent.SwitchBranch -> switchBranch(intent.sessionId)
            is ChatIntent.ReconnectMcp -> viewModelScope.launch { loadToolDefinitions() }
        }
    }

    fun onNavigatingBack(navigate: () -> Unit) {
        if (sessionManager.currentGroupId == null) {
            navigate()
            return
        }
        viewModelScope.launch {
            sessionManager.cleanupGroupOnExit(activeChatId, _state.value.messages.isEmpty())
            navigate()
        }
    }

    private fun switchBranch(sessionId: String) {
        if (sessionId == activeChatId || _state.value.isLoading) return
        viewModelScope.launch {
            val session = sessionManager.switchBranch(sessionId) ?: return@launch
            _activeChatId.value = sessionId
            _state.update {
                it.copy(
                    messages = session.messages,
                    activeBranchIndex = session.branchIndex,
                    activeChatId = sessionId,
                    inputText = "",
                    error = null,
                    chatMetrics = null,
                    isPeriodicTask = session.isPeriodicTask
                )
            }
        }
    }

    private fun createCheckpoint() {
        if (_state.value.isLoading) return
        if (_state.value.branches.size >= 2) return
        viewModelScope.launch {
            val branches = sessionManager.createCheckpoint(activeChatId, _state.value.currentProfileId)
            if (branches.isNotEmpty()) {
                _state.update {
                    it.copy(
                        branches = branches,
                        activeBranchIndex = branches.firstOrNull { b -> b.sessionId == activeChatId }?.branchIndex ?: it.activeBranchIndex
                    )
                }
            }
        }
    }

    private fun clearChat() {
        _state.update {
            it.copy(
                showMetrics = false,
                messages = emptyList(),
                error = null,
                isPeriodicTask = false
            )
        }
        viewModelScope.launch {
            metricsRepository.deleteMetrics(activeChatId)
            persistSession(emptyList())
            val clearResult = sessionManager.clearSessionData()
            if (clearResult.newActiveChatId != null) {
                _activeChatId.value = clearResult.newActiveChatId
                _state.update {
                    it.copy(
                        messages = clearResult.newMessages ?: emptyList(),
                        activeChatId = clearResult.newActiveChatId,
                        branches = emptyList(),
                        activeBranchIndex = 0
                    )
                }
            } else {
                _state.update { it.copy(branches = emptyList(), activeBranchIndex = 0) }
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
            val profile = _state.value.currentProfileId?.let { userProfileRepository.getById(it) }
            val globalPrefix = profile?.description ?: ""

            val constraints = profile?.constraints ?: emptyList()
            val tools = cachedToolDefinitions

            val doStickyFacts = settings.stickyFactsEnabled
                && existingMessages.size > settings.stickyFactsRecentMessages
            val doSummary = settings.summaryEnabled
                && existingMessages.size > settings.retainedMessageCount
            if (doStickyFacts) {
                sendMessageWithStickyFacts(settings, globalPrefix, existingMessages, userMessage, constraints, tools)
            } else if (doSummary) {
                sendMessageWithSummary(settings, globalPrefix, existingMessages, userMessage, constraints, tools)
            } else {
                sendMessageNormal(settings, globalPrefix, constraints, tools)
            }
        }
    }

    private fun effectiveSystemPrompt(
        globalPrefix: String,
        chatPrompt: String,
        constraints: List<Constraint> = emptyList()
    ): String {
        return buildSystemPromptUseCase(
            globalPrefix = globalPrefix,
            chatPrompt = chatPrompt,
            constraints = constraints
        )
    }

    private suspend fun sendAndProcessResponse(
        result: Result<ChatResult>,
        settings: ChatSettings,
        globalPrefix: String,
        constraints: List<Constraint>,
        tools: List<ToolDefinition>?,
        buildHistory: (messages: List<ChatMessage>) -> List<ChatMessage>,
        buildFinalMessages: (assistantContent: String, currentMessages: List<ChatMessage>) -> List<ChatMessage>
    ) {
        result.fold(
            onSuccess = { initialResult ->
                val finalResult = try {
                    processToolCallingLoop(initialResult, settings, tools, buildHistory)
                } catch (e: Exception) {
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Tool calling failed") }
                    return
                }

                handleResponseWithConstraints(finalResult.message, constraints, settings, globalPrefix) { finalContent ->
                    val currentMessages = _state.value.messages
                    val finalMessages = buildFinalMessages(finalContent, currentMessages)
                    _state.update { it.copy(messages = finalMessages, isLoading = false) }

                    persistSession(finalMessages)
                    updateMetrics(finalResult.metrics)
                }
            },
            onFailure = { throwable ->
                _state.update { it.copy(isLoading = false, error = throwable.message ?: "Unknown error") }
            }
        )
    }

    private suspend fun processToolCallingLoop(
        initialResult: ChatResult,
        settings: ChatSettings,
        tools: List<ToolDefinition>?,
        buildHistory: (messages: List<ChatMessage>) -> List<ChatMessage>
    ): ChatResult {
        var result = initialResult
        var iterations = 0
        while (result.finishReason == ChatResult.FINISH_REASON_TOOL_CALLS && result.toolCalls != null && iterations < MAX_TOOL_ITERATIONS) {
            iterations++
            Timber.tag("ChatViewModel").d("Tool calling iteration $iterations, ${result.toolCalls!!.size} tools to call")

            val toolCallsJson = Json.encodeToString(result.toolCalls!!)
            val toolCallMessage = ChatMessage(
                role = ChatMessage.ROLE_TOOL_CALL,
                content = toolCallsJson
            )
            _state.update { it.copy(messages = it.messages + toolCallMessage) }

            val executionResult = executeToolCallsUseCase(result.toolCalls!!, activeChatId)
            if (executionResult.hadPeriodicTaskTools) {
                _state.update { it.copy(isPeriodicTask = true) }
            }
            _state.update { it.copy(messages = it.messages + executionResult.messages) }

            val currentMessages = _state.value.messages
            val fullHistory = buildHistory(currentMessages)
            val nextResult = sendChatMessageUseCase(
                fullHistory,
                settings.maxTokens,
                settings.temperature,
                settings.model.id,
                tools
            )

            if (nextResult.isFailure) {
                throw nextResult.exceptionOrNull() ?: Exception("Tool calling loop failed")
            }
            result = nextResult.getOrThrow()
        }

        return result
    }

    private suspend fun sendMessageNormal(
        settings: ChatSettings,
        globalPrefix: String,
        constraints: List<Constraint> = emptyList(),
        tools: List<ToolDefinition>? = null
    ) {
        fun buildHistory(messages: List<ChatMessage>): List<ChatMessage> = buildList {
            add(ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = effectiveSystemPrompt(globalPrefix, settings.systemPrompt, constraints)))
            addAll(messages)
        }

        val result = sendChatMessageUseCase(buildHistory(_state.value.messages), settings.maxTokens, settings.temperature, settings.model.id, tools)

        sendAndProcessResponse(result, settings, globalPrefix, constraints, tools, ::buildHistory) { finalContent, currentMessages ->
            val assistantMessage = ChatMessage(role = ChatMessage.ROLE_ASSISTANT, content = finalContent)
            val messagesWithResponse = currentMessages + assistantMessage
            if (settings.slidingWindowEnabled && messagesWithResponse.size > settings.slidingWindowSize) {
                messagesWithResponse.takeLast(settings.slidingWindowSize)
            } else {
                messagesWithResponse
            }
        }
    }

    private suspend fun sendMessageWithSummary(
        settings: ChatSettings,
        globalPrefix: String,
        existingMessages: List<ChatMessage>,
        userMessage: ChatMessage,
        constraints: List<Constraint> = emptyList(),
        tools: List<ToolDefinition>? = null
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
                val label = if (msg.role == ChatMessage.ROLE_ASSISTANT) PromptTemplates.ROLE_LABEL_ASSISTANT else PromptTemplates.ROLE_LABEL_USER
                "$label: ${msg.content}"
            })
        }
        val summaryHistory = listOf(
            ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = PromptTemplates.SUMMARIZER_SYSTEM_PROMPT),
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
                it.copy(isLoading = false, error = summaryResult.exceptionOrNull()?.message ?: "Summary request failed")
            }
            return
        }

        val summaryContent = summaryResult.getOrThrow().message

        fun buildHistory(messages: List<ChatMessage>): List<ChatMessage> = buildList {
            add(ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = "${effectiveSystemPrompt(globalPrefix, settings.systemPrompt, constraints)}\n\nКонтекст предыдущих сообщений:\n$summaryContent"))
            addAll(recentMessages.filter { it.role != ChatMessage.ROLE_SUMMARY })
            add(userMessage)
            val toolMessages = messages.drop(existingMessages.size + 1)
            addAll(toolMessages)
        }

        val mainResult = sendChatMessageUseCase(buildHistory(_state.value.messages), settings.maxTokens, settings.temperature, settings.model.id, tools)

        sendAndProcessResponse(mainResult, settings, globalPrefix, constraints, tools, ::buildHistory) { finalContent, _ ->
            val summaryMessage = ChatMessage(role = ChatMessage.ROLE_SUMMARY, content = summaryContent)
            val assistantMessage = ChatMessage(role = ChatMessage.ROLE_ASSISTANT, content = finalContent)
            listOf(summaryMessage) + recentMessages + userMessage + assistantMessage
        }
    }

    private suspend fun sendMessageWithStickyFacts(
        settings: ChatSettings,
        globalPrefix: String,
        existingMessages: List<ChatMessage>,
        userMessage: ChatMessage,
        constraints: List<Constraint> = emptyList(),
        tools: List<ToolDefinition>? = null
    ) {
        val recentMessages = existingMessages.takeLast(settings.stickyFactsRecentMessages)
        val olderMessages = existingMessages.dropLast(settings.stickyFactsRecentMessages)

        val previousFacts = olderMessages.firstOrNull { it.role == ChatMessage.ROLE_FACTS }
        val olderNonFacts = olderMessages.filter { it.role != ChatMessage.ROLE_FACTS }

        val conversationText = buildString {
            if (previousFacts != null) append("${previousFacts.content}\n\n")
            append(olderNonFacts.joinToString("\n") { msg ->
                val label = if (msg.role == ChatMessage.ROLE_ASSISTANT) PromptTemplates.ROLE_LABEL_ASSISTANT else PromptTemplates.ROLE_LABEL_USER
                "$label: ${msg.content}"
            })
        }

        val factsHistory = listOf(
            ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = PromptTemplates.FACTS_EXTRACTOR_SYSTEM_PROMPT),
            ChatMessage(role = ChatMessage.ROLE_USER, content = "Сделай краткую выдержку важных данных следующей переписки в формате факт:значение. Анализируй текст, который идет после слова Контент. Каждый новый факт переноси на новую строку, чтобы выглядело аккуратно. Контент:\n\n$conversationText")
        )
        val factsResult = sendChatMessageUseCase(factsHistory, maxTokens = null, temperature = null, model = settings.model.id)

        if (factsResult.isFailure) {
            _state.update { it.copy(isLoading = false, error = factsResult.exceptionOrNull()?.message ?: "Facts extraction failed") }
            return
        }

        val factsContent = factsResult.getOrThrow().message

        fun buildHistory(messages: List<ChatMessage>): List<ChatMessage> = buildList {
            add(ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = effectiveSystemPrompt(globalPrefix, settings.systemPrompt, constraints)))
            add(ChatMessage(role = ChatMessage.ROLE_USER, content = factsContent))
            addAll(recentMessages.filter { it.role != ChatMessage.ROLE_FACTS })
            add(userMessage)
            val toolMessages = messages.drop(existingMessages.size + 1)
            addAll(toolMessages)
        }

        val mainResult = sendChatMessageUseCase(buildHistory(_state.value.messages), settings.maxTokens, settings.temperature, settings.model.id, tools)

        sendAndProcessResponse(mainResult, settings, globalPrefix, constraints, tools, ::buildHistory) { finalContent, _ ->
            val factsMessage = ChatMessage(role = ChatMessage.ROLE_FACTS, content = factsContent)
            val assistantMessage = ChatMessage(role = ChatMessage.ROLE_ASSISTANT, content = finalContent)
            listOf(factsMessage) + recentMessages.filter { it.role != ChatMessage.ROLE_FACTS } + userMessage + assistantMessage
        }
    }

    private suspend fun handleResponseWithConstraints(
        responseMessage: String,
        constraints: List<Constraint>,
        settings: ChatSettings,
        globalPrefix: String,
        onSuccess: suspend (String) -> Unit
    ) {
        var currentResponse = responseMessage
        var retryCount = 0

        while (retryCount < MAX_CONSTRAINT_RETRIES) {
            val violations = validateConstraintsUseCase(currentResponse, constraints)
            if (violations.isEmpty()) break

            val violationNames = violations.joinToString(", ") { it.name }
            val assistantMsg = ChatMessage(role = ChatMessage.ROLE_CONSTRAINT_VIOLATION_ASSISTANT, content = currentResponse)
            val violationNotice = ChatMessage(
                role = ChatMessage.ROLE_CONSTRAINT_VIOLATION_USER,
                content = "Твой ответ нарушил следующие ограничения: $violationNames. Перегенерируй ответ, соблюдая все ограничения."
            )
            _state.update { it.copy(messages = it.messages + assistantMsg + violationNotice) }

            val retryHistory = buildList {
                add(ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = effectiveSystemPrompt(globalPrefix, settings.systemPrompt, constraints)))
                addAll(_state.value.messages)
            }
            val retryResult = sendChatMessageUseCase(retryHistory, settings.maxTokens, settings.temperature, settings.model.id)
            if (retryResult.isFailure) {
                _state.update { it.copy(isLoading = false, error = retryResult.exceptionOrNull()?.message ?: "Retry failed") }
                return
            }
            currentResponse = retryResult.getOrThrow().message
            retryCount++
        }

        if (retryCount == MAX_CONSTRAINT_RETRIES) {
            val violations = validateConstraintsUseCase(currentResponse, constraints)
            if (violations.isNotEmpty()) {
                val violationNames = violations.joinToString(", ") { it.name }
                currentResponse += "\n\n[Не удалось выполнить ограничения после $MAX_CONSTRAINT_RETRIES попыток: $violationNames]"
            }
        }

        onSuccess(currentResponse)
    }

    private suspend fun updateMetrics(responseMetrics: com.example.aichallengeapp.core.database.domain.model.ResponseMetrics) {
        val currentTotal = _state.value.chatMetrics?.totalTokens ?: 0
        updateMetricsUseCase(activeChatId, currentTotal, responseMetrics)
    }

    private suspend fun persistSession(messages: List<ChatMessage>) {
        val s = _state.value
        sessionManager.persistSession(
            chatId = activeChatId,
            messages = messages,
            profileId = s.currentProfileId,
            isPeriodicTask = s.isPeriodicTask
        )
    }
}
