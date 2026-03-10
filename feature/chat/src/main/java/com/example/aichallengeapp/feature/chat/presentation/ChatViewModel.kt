package com.example.aichallengeapp.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.ChatResult
import com.example.aichallengeapp.core.database.domain.model.ChatSession
import com.example.aichallengeapp.core.database.domain.model.Constraint
import com.example.aichallengeapp.core.database.domain.model.TaskStage
import com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository
import com.example.aichallengeapp.core.database.domain.repository.ChatSessionRepository
import com.example.aichallengeapp.core.database.domain.repository.UserProfileRepository
import com.example.aichallengeapp.core.mcp.model.ToolDefinition
import kotlinx.serialization.json.Json
import com.example.aichallengeapp.feature.chat.domain.PromptTemplates
import com.example.aichallengeapp.feature.chat.domain.usecase.BuildSystemPromptUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.DetectNewTaskUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.DetectStageTransitionUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.ExecuteToolCallsUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.GenerateStageArtifactUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.GetToolDefinitionsUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.SendChatMessageUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.UpdateMetricsUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.ValidateConstraintsUseCase
import com.example.aichallengeapp.feature.settings.domain.model.ChatSettings
import com.example.aichallengeapp.feature.settings.domain.repository.ChatSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

private const val MAX_CONSTRAINT_RETRIES = 3
private const val MAX_BRANCHES = 2
private const val FIRST_BRANCH_INDEX = 1
private const val STAGE_LOG_TAG = "StageArtifacts"
private const val MAX_TOOL_ITERATIONS = 5

class ChatViewModel(
    private val chatId: String,
    private val initialBranchIndex: Int,
    private val initialProfileId: String?,
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val detectStageTransitionUseCase: DetectStageTransitionUseCase,
    private val detectNewTaskUseCase: DetectNewTaskUseCase,
    private val generateStageArtifactUseCase: GenerateStageArtifactUseCase,
    private val validateConstraintsUseCase: ValidateConstraintsUseCase,
    private val buildSystemPromptUseCase: BuildSystemPromptUseCase,
    private val updateMetricsUseCase: UpdateMetricsUseCase,
    private val executeToolCallsUseCase: ExecuteToolCallsUseCase,
    private val getToolDefinitionsUseCase: GetToolDefinitionsUseCase,
    private val settingsRepository: ChatSettingsRepository,
    private val sessionRepository: ChatSessionRepository,
    private val metricsRepository: ChatMetricsRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _activeChatId = MutableStateFlow(chatId)
    private val activeChatId: String get() = _activeChatId.value

    private var currentGroupId: String? = null
    private var currentTask: String? = null
    private var currentProfileId: String? = null
    private val currentStageArtifacts: MutableMap<TaskStage, String> = mutableMapOf()
    private var cachedSession: ChatSession? = null
    private var cachedToolDefinitions: List<ToolDefinition>? = null

    init {
        viewModelScope.launch {
            val session = sessionRepository.getSession(chatId)
            if (session != null) {
                cachedSession = session
                currentProfileId = session.profileId ?: initialProfileId
                val groupId = session.checkpointGroupId
                if (groupId != null && session.branchIndex != initialBranchIndex) {
                    val target = sessionRepository.getSessionsByGroup(groupId)
                        .firstOrNull { it.branchIndex == initialBranchIndex }
                    if (target != null) {
                        cachedSession = target
                        _activeChatId.value = target.id
                        _state.update { it.copy(messages = target.messages, activeChatId = target.id, currentTaskStage = target.currentTaskStage, currentTask = target.currentTask) }
                        currentTask = target.currentTask
                        currentStageArtifacts.clear()
                        currentStageArtifacts.putAll(target.stageArtifacts)
                        loadBranches(groupId, initialBranchIndex)
                        loadProfileName()
                        return@launch
                    }
                }
                _state.update { it.copy(messages = session.messages, activeChatId = chatId, currentTaskStage = session.currentTaskStage, currentTask = session.currentTask) }
                currentTask = session.currentTask
                currentStageArtifacts.clear()
                currentStageArtifacts.putAll(session.stageArtifacts)
                if (groupId != null) loadBranches(groupId, session.branchIndex)
            } else {
                currentProfileId = initialProfileId
                _state.update { it.copy(activeChatId = chatId) }
            }
            loadProfileName()
        }
        viewModelScope.launch {
            _activeChatId
                .flatMapLatest { id -> metricsRepository.observeMetrics(id) }
                .collect { metrics -> _state.update { it.copy(chatMetrics = metrics) } }
        }
        // Pre-load tool definitions in background
        viewModelScope.launch { loadToolDefinitions() }
    }

    private suspend fun loadToolDefinitions() {
        try {
            cachedToolDefinitions = getToolDefinitionsUseCase()
            val count = cachedToolDefinitions?.size ?: 0
            Timber.tag("ChatViewModel").d("Loaded $count tool definitions")
            _state.update { it.copy(mcpToolsCount = count) }
        } catch (e: Exception) {
            Timber.tag("ChatViewModel").w(e, "MCP server unavailable, continuing without tools")
            _state.update { it.copy(mcpToolsCount = 0) }
        }
    }

    private fun reconnectMcp() {
        viewModelScope.launch { loadToolDefinitions() }
    }

    private suspend fun loadProfileName() {
        val profile = currentProfileId?.let { userProfileRepository.getById(it) }
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
            is ChatIntent.ReconnectMcp -> reconnectMcp()
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
            cachedSession = session
            _activeChatId.value = sessionId
            currentTask = session.currentTask
            currentStageArtifacts.clear()
            currentStageArtifacts.putAll(session.stageArtifacts)
            _state.update {
                it.copy(
                    messages = session.messages,
                    activeBranchIndex = session.branchIndex,
                    activeChatId = sessionId,
                    inputText = "",
                    error = null,
                    chatMetrics = null,
                    currentTaskStage = session.currentTaskStage,
                    currentTask = session.currentTask
                )
            }
        }
    }

    private suspend fun cleanupGroupOnExit(groupId: String) {
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
                groupId = requireNotNull(current.checkpointGroupId) { "checkpointGroupId must not be null in else branch" }
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
                    branchIndex = nextBranchIndex,
                    profileId = currentProfileId
                )
            )
            loadBranches(groupId, currentBranchIndex)
        }
    }

    private fun clearChat() {
        currentTask = null
        currentStageArtifacts.clear()
        _state.update { it.copy(showMetrics = false, messages = emptyList(), error = null, currentTaskStage = TaskStage.PLANNING, currentTask = null) }
        viewModelScope.launch {
            metricsRepository.deleteMetrics(activeChatId)
            persistSession(emptyList())
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
            val profile = currentProfileId?.let { userProfileRepository.getById(it) }
            val globalPrefix = profile?.description ?: ""

            processStageAndTaskDetection(existingMessages, userMessage, settings.model.id)

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

    private suspend fun processStageAndTaskDetection(
        existingMessages: List<ChatMessage>,
        userMessage: ChatMessage,
        model: String
    ) {
        val currentStage = _state.value.currentTaskStage
        val newStage = detectStageTransitionUseCase(existingMessages, userMessage, currentStage, model)
        if (newStage != null && newStage != currentStage) {
            val allStages = TaskStage.entries
            val oldIndex = allStages.indexOf(currentStage)
            val newIndex = allStages.indexOf(newStage)
            var newArtifact: String? = null
            if (newIndex > oldIndex) {
                val artifact = generateStageArtifactUseCase(currentStage, _state.value.messages, model)
                currentStageArtifacts[currentStage] = artifact
                newArtifact = artifact
            } else {
                currentStageArtifacts.remove(newStage)
            }
            logStageTransition(currentStage, newStage, newArtifact)
            _state.update { it.copy(currentTaskStage = newStage) }
        } else {
            val detectedTask = detectNewTaskUseCase(existingMessages, userMessage, currentStage, model)
            if (detectedTask != null) {
                currentTask = detectedTask
                currentStageArtifacts.clear()
                _state.update { it.copy(currentTaskStage = TaskStage.PLANNING, currentTask = detectedTask) }
            }
        }
    }

    private fun effectiveSystemPrompt(
        globalPrefix: String,
        chatPrompt: String,
        constraints: List<Constraint> = emptyList()
    ): String = buildSystemPromptUseCase(
        globalPrefix = globalPrefix,
        chatPrompt = chatPrompt,
        currentTaskStage = _state.value.currentTaskStage,
        stageArtifacts = currentStageArtifacts.toMap(),
        constraints = constraints,
        currentTask = currentTask
    )

    private fun logStageTransition(from: TaskStage, to: TaskStage, newArtifact: String?) {
        val log = Timber.tag(STAGE_LOG_TAG)
        val sep = "─".repeat(36)
        log.d("┌$sep")
        log.d("│  STAGE TRANSITION")
        log.d("│  {${from.name}} → {${to.name}}")
        if (newArtifact != null) {
            log.d("├─── ARTIFACT [${from.name}] ${"─".repeat(16)}")
            newArtifact.lines().forEach { log.d("│  $it") }
        }
        if (currentStageArtifacts.isNotEmpty()) {
            log.d("├─── ACTIVE ARTIFACTS ${"─".repeat(15)}")
            currentStageArtifacts.forEach { (stage, artifact) ->
                artifact.lines().forEachIndexed { i, line ->
                    if (i == 0) log.d("│  [${stage.name}] $line")
                    else log.d("│  $line")
                }
            }
        }
        log.d("└$sep")
    }

    private suspend fun sendAndProcessResponse(
        result: Result<ChatResult>,
        settings: ChatSettings,
        globalPrefix: String,
        constraints: List<Constraint>,
        tools: List<ToolDefinition>?,
        buildHistory: () -> List<ChatMessage>,
        buildFinalMessages: (assistantContent: String) -> List<ChatMessage>
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
                    val finalMessages = buildFinalMessages(finalContent)
                    _state.update { it.copy(messages = finalMessages, isLoading = false, currentTask = currentTask) }
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
        buildHistory: () -> List<ChatMessage>
    ): ChatResult {
        var result = initialResult
        var iterations = 0

        while (result.finishReason == "tool_calls" && result.toolCalls != null && iterations < MAX_TOOL_ITERATIONS) {
            iterations++
            Timber.tag("ChatViewModel").d("Tool calling iteration $iterations, ${result.toolCalls!!.size} tools to call")

            // Add the assistant's tool_call message to state (hidden in UI)
            // Content stores serialized ToolCallInfo list for reconstruction in ChatRepositoryImpl
            val toolCallsJson = Json.encodeToString(result.toolCalls!!)
            val toolCallMessage = ChatMessage(
                role = ChatMessage.ROLE_TOOL_CALL,
                content = toolCallsJson
            )
            _state.update { it.copy(messages = it.messages + toolCallMessage) }

            // Execute all tool calls
            val toolResultMessages = executeToolCallsUseCase(result.toolCalls!!)
            _state.update { it.copy(messages = it.messages + toolResultMessages) }

            // Rebuild history with tool results and send again
            val fullHistory = buildHistory()
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
        fun buildHistory(): List<ChatMessage> = buildList {
            add(ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = effectiveSystemPrompt(globalPrefix, settings.systemPrompt, constraints)))
            addAll(_state.value.messages)
        }

        val result = sendChatMessageUseCase(buildHistory(), settings.maxTokens, settings.temperature, settings.model.id, tools)

        sendAndProcessResponse(result, settings, globalPrefix, constraints, tools, ::buildHistory) { finalContent ->
            val assistantMessage = ChatMessage(role = ChatMessage.ROLE_ASSISTANT, content = finalContent)
            val messagesWithResponse = _state.value.messages + assistantMessage
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

        fun buildHistory(): List<ChatMessage> = buildList {
            add(ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = "${effectiveSystemPrompt(globalPrefix, settings.systemPrompt, constraints)}\n\nКонтекст предыдущих сообщений:\n$summaryContent"))
            addAll(recentMessages.filter { it.role != ChatMessage.ROLE_SUMMARY })
            add(userMessage)
        }

        val mainResult = sendChatMessageUseCase(buildHistory(), settings.maxTokens, settings.temperature, settings.model.id, tools)

        sendAndProcessResponse(mainResult, settings, globalPrefix, constraints, tools, ::buildHistory) { finalContent ->
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

        fun buildHistory(): List<ChatMessage> = buildList {
            add(ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = effectiveSystemPrompt(globalPrefix, settings.systemPrompt, constraints)))
            add(ChatMessage(role = ChatMessage.ROLE_USER, content = factsContent))
            addAll(recentMessages.filter { it.role != ChatMessage.ROLE_FACTS })
            add(userMessage)
        }

        val mainResult = sendChatMessageUseCase(buildHistory(), settings.maxTokens, settings.temperature, settings.model.id, tools)

        sendAndProcessResponse(mainResult, settings, globalPrefix, constraints, tools, ::buildHistory) { finalContent ->
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
        if (messages.isEmpty()) {
            cachedSession = null
            sessionRepository.deleteSession(activeChatId)
        } else {
            val session = (cachedSession ?: ChatSession(id = activeChatId)).copy(
                id = activeChatId,
                messages = messages,
                updatedAt = System.currentTimeMillis(),
                currentTask = currentTask,
                currentTaskStage = _state.value.currentTaskStage,
                profileId = currentProfileId,
                stageArtifacts = currentStageArtifacts.toMap()
            )
            cachedSession = session
            sessionRepository.upsertSession(session)
        }
    }
}
