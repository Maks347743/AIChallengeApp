package com.example.aichallengeapp.feature.chat.presentation

import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.ChatMetrics
import com.example.aichallengeapp.core.database.domain.model.TaskStage

sealed interface ChatIntent {
    data object SendMessage : ChatIntent
    data class UpdateInput(val text: String) : ChatIntent
    data object ClearChat : ChatIntent
    data object ToggleMetrics : ChatIntent
    data object CreateCheckpoint : ChatIntent
    data class SwitchBranch(val sessionId: String) : ChatIntent
    data object ReconnectMcp : ChatIntent
}

data class BranchInfo(val sessionId: String, val branchIndex: Int)

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val chatMetrics: ChatMetrics? = null,
    val showMetrics: Boolean = false,
    val branches: List<BranchInfo> = emptyList(),
    val activeBranchIndex: Int = 0,
    val activeChatId: String = "",
    val currentProfileName: String? = null,
    val currentTaskStage: TaskStage = TaskStage.PLANNING,
    val currentTask: String? = null,
    val mcpToolsCount: Int = 0
)
