package com.example.aichallengeapp.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.ChatSession
import com.example.aichallengeapp.core.database.domain.model.Constraint
import com.example.aichallengeapp.core.database.domain.model.TaskStage
import com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository
import com.example.aichallengeapp.core.database.domain.repository.ChatSessionRepository
import com.example.aichallengeapp.core.database.domain.repository.UserProfileRepository
import com.example.aichallengeapp.feature.chat.domain.PromptTemplates
import com.example.aichallengeapp.feature.chat.domain.usecase.BuildSystemPromptUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.DetectNewTaskUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.DetectStageTransitionUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.GenerateStageArtifactUseCase
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
    private var currentTaskStage: TaskStage = TaskStage.PLANNING
    private var currentProfileId: String? = null
    private val currentStageArtifacts: MutableMap<TaskStage, String> = mutableMapOf()
    private var cachedSession: ChatSession? = null

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
                        currentTaskStage = target.currentTaskStage
                        currentStageArtifacts.clear()
                        currentStageArtifacts.putAll(target.stageArtifacts)
                        loadBranches(groupId, initialBranchIndex)
                        loadProfileName()
                        return@launch
                    }
                }
                _state.update { it.copy(messages = session.messages, activeChatId = chatId, currentTaskStage = session.currentTaskStage, currentTask = session.currentTask) }
                currentTask = session.currentTask
                currentTaskStage = session.currentTaskStage
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
            currentTaskStage = session.currentTaskStage
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
        currentTaskStage = TaskStage.PLANNING
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

            val doStickyFacts = settings.stickyFactsEnabled
                && existingMessages.size > settings.stickyFactsRecentMessages
            val doSummary = settings.summaryEnabled
                && existingMessages.size > settings.retainedMessageCount
            if (doStickyFacts) {
                sendMessageWithStickyFacts(settings, globalPrefix, existingMessages, userMessage, constraints)
            } else if (doSummary) {
                sendMessageWithSummary(settings, globalPrefix, existingMessages, userMessage, constraints)
            } else {
                sendMessageNormal(settings, globalPrefix, constraints)
            }
        }
    }

    private suspend fun processStageAndTaskDetection(
        existingMessages: List<ChatMessage>,
        userMessage: ChatMessage,
        model: String
    ) {
        val newStage = detectStageTransitionUseCase(existingMessages, userMessage, currentTaskStage, model)
        if (newStage != null && newStage != currentTaskStage) {
            val oldStage = currentTaskStage
            val allStages = TaskStage.entries
            val oldIndex = allStages.indexOf(oldStage)
            val newIndex = allStages.indexOf(newStage)
            var newArtifact: String? = null
            if (newIndex > oldIndex) {
                val artifact = generateStageArtifactUseCase(oldStage, _state.value.messages, model)
                currentStageArtifacts[oldStage] = artifact
                newArtifact = artifact
            } else {
                currentStageArtifacts.remove(newStage)
            }
            logStageTransition(oldStage, newStage, newArtifact)
            currentTaskStage = newStage
            _state.update { it.copy(currentTaskStage = newStage) }
        } else {
            val detectedTask = detectNewTaskUseCase(existingMessages, userMessage, currentTaskStage, model)
            if (detectedTask != null) {
                currentTask = detectedTask
                currentTaskStage = TaskStage.PLANNING
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
        currentTaskStage = currentTaskStage,
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

    private suspend fun sendMessageNormal(
        settings: ChatSettings,
        globalPrefix: String,
        constraints: List<Constraint> = emptyList()
    ) {
        val fullHistory = buildList {
            add(ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = effectiveSystemPrompt(globalPrefix, settings.systemPrompt, constraints)))
            addAll(_state.value.messages)
        }

        sendChatMessageUseCase(fullHistory, settings.maxTokens, settings.temperature, settings.model.id)
            .onSuccess { result ->
                handleResponseWithConstraints(result.message, constraints, settings, globalPrefix) { finalContent ->
                    val assistantMessage = ChatMessage(role = ChatMessage.ROLE_ASSISTANT, content = finalContent)
                    val messagesWithResponse = _state.value.messages + assistantMessage
                    val finalMessages = if (settings.slidingWindowEnabled
                        && messagesWithResponse.size > settings.slidingWindowSize
                    ) {
                        messagesWithResponse.takeLast(settings.slidingWindowSize)
                    } else {
                        messagesWithResponse
                    }
                    _state.update { it.copy(messages = finalMessages, isLoading = false, currentTaskStage = currentTaskStage, currentTask = currentTask) }
                    persistSession(finalMessages)
                    updateMetrics(result.metrics)
                }
            }
            .onFailure { throwable ->
                _state.update { it.copy(isLoading = false, error = throwable.message ?: "Unknown error") }
            }
    }

    private suspend fun sendMessageWithSummary(
        settings: ChatSettings,
        globalPrefix: String,
        existingMessages: List<ChatMessage>,
        userMessage: ChatMessage,
        constraints: List<Constraint> = emptyList()
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

        val mainHistory = buildList {
            add(ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = "${effectiveSystemPrompt(globalPrefix, settings.systemPrompt, constraints)}\n\nКонтекст предыдущих сообщений:\n$summaryContent"))
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
            handleResponseWithConstraints(result.message, constraints, settings, globalPrefix) { finalContent ->
                val summaryMessage = ChatMessage(role = ChatMessage.ROLE_SUMMARY, content = summaryContent)
                val assistantMessage = ChatMessage(role = ChatMessage.ROLE_ASSISTANT, content = finalContent)
                val newMessages = listOf(summaryMessage) + recentMessages + userMessage + assistantMessage
                _state.update { it.copy(messages = newMessages, isLoading = false, currentTaskStage = currentTaskStage, currentTask = currentTask) }
                persistSession(newMessages)
                updateMetrics(result.metrics)
            }
        }
        mainResult.onFailure { throwable ->
            _state.update { it.copy(isLoading = false, error = throwable.message ?: "Unknown error") }
        }
    }

    private suspend fun sendMessageWithStickyFacts(
        settings: ChatSettings,
        globalPrefix: String,
        existingMessages: List<ChatMessage>,
        userMessage: ChatMessage,
        constraints: List<Constraint> = emptyList()
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

        val mainHistory = buildList {
            add(ChatMessage(role = ChatMessage.ROLE_SYSTEM, content = effectiveSystemPrompt(globalPrefix, settings.systemPrompt, constraints)))
            add(ChatMessage(role = ChatMessage.ROLE_USER, content = factsContent))
            addAll(recentMessages.filter { it.role != ChatMessage.ROLE_FACTS })
            add(userMessage)
        }
        val mainResult = sendChatMessageUseCase(mainHistory, settings.maxTokens, settings.temperature, settings.model.id)

        mainResult.onSuccess { result ->
            handleResponseWithConstraints(result.message, constraints, settings, globalPrefix) { finalContent ->
                val factsMessage = ChatMessage(role = ChatMessage.ROLE_FACTS, content = factsContent)
                val assistantMessage = ChatMessage(role = ChatMessage.ROLE_ASSISTANT, content = finalContent)
                val newMessages = listOf(factsMessage) +
                    recentMessages.filter { it.role != ChatMessage.ROLE_FACTS } +
                    userMessage + assistantMessage
                _state.update { it.copy(messages = newMessages, isLoading = false, currentTaskStage = currentTaskStage, currentTask = currentTask) }
                persistSession(newMessages)
                updateMetrics(result.metrics)
            }
        }
        mainResult.onFailure { throwable ->
            _state.update { it.copy(isLoading = false, error = throwable.message ?: "Unknown error") }
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
                currentTaskStage = currentTaskStage,
                profileId = currentProfileId,
                stageArtifacts = currentStageArtifacts.toMap()
            )
            cachedSession = session
            sessionRepository.upsertSession(session)
        }
    }
}
