package com.example.aichallengeapp.feature.chatlist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichallengeapp.core.database.domain.model.ChatSession
import com.example.aichallengeapp.core.database.domain.model.UserProfile
import com.example.aichallengeapp.core.database.domain.repository.ChatSessionRepository
import com.example.aichallengeapp.core.database.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.UUID

private const val SESSIONS_FLOW_TIMEOUT_MS = 5000L

class ChatListViewModel(
    sessionRepository: ChatSessionRepository,
    userProfileRepository: UserProfileRepository
) : ViewModel() {

    val sessions: StateFlow<List<ChatSession>> = sessionRepository.getAllSessions()
        .map { list -> list.filter { it.messages.isNotEmpty() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SESSIONS_FLOW_TIMEOUT_MS), emptyList())

    val profiles: StateFlow<List<UserProfile>> = userProfileRepository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SESSIONS_FLOW_TIMEOUT_MS), emptyList())

    fun newSessionId(): String = UUID.randomUUID().toString()
}
