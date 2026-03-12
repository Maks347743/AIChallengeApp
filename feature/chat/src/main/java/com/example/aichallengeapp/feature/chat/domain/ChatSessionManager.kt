package com.example.aichallengeapp.feature.chat.domain

import com.example.aichallengeapp.core.database.domain.model.ChatSession
import com.example.aichallengeapp.core.database.domain.repository.ChatSessionRepository
import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.feature.chat.presentation.BranchInfo
import java.util.UUID

private const val MAX_BRANCHES = 2
private const val FIRST_BRANCH_INDEX = 1

class ChatSessionManager(
    private val sessionRepository: ChatSessionRepository
) {
    var cachedSession: ChatSession? = null
        private set
    var currentGroupId: String? = null
        private set

    data class LoadResult(
        val session: ChatSession?,
        val targetSession: ChatSession?,
        val branches: List<BranchInfo> = emptyList(),
        val activeBranchIndex: Int = 0
    )

    suspend fun loadSession(
        chatId: String,
        initialBranchIndex: Int
    ): LoadResult {
        val session = sessionRepository.getSession(chatId) ?: return LoadResult(null, null)
        cachedSession = session
        val groupId = session.checkpointGroupId

        if (groupId != null && session.branchIndex != initialBranchIndex) {
            val target = sessionRepository.getSessionsByGroup(groupId)
                .firstOrNull { it.branchIndex == initialBranchIndex }
            if (target != null) {
                cachedSession = target
                val branches = loadBranchesInternal(groupId)
                return LoadResult(session, target, branches, initialBranchIndex)
            }
        }

        val branches = if (groupId != null) {
            loadBranchesInternal(groupId)
        } else {
            emptyList()
        }

        return LoadResult(session, null, branches, session.branchIndex)
    }

    suspend fun switchBranch(sessionId: String): ChatSession? {
        val session = sessionRepository.getSession(sessionId) ?: return null
        cachedSession = session
        return session
    }

    suspend fun persistSession(
        chatId: String,
        messages: List<ChatMessage>,
        profileId: String?,
        isPeriodicTask: Boolean
    ) {
        if (messages.isEmpty()) {
            cachedSession = null
            sessionRepository.deleteSession(chatId)
        } else {
            val session = (cachedSession ?: ChatSession(id = chatId)).copy(
                id = chatId,
                messages = messages,
                updatedAt = System.currentTimeMillis(),
                profileId = profileId,
                isPeriodicTask = isPeriodicTask
            )
            cachedSession = session
            sessionRepository.upsertSession(session)
        }
    }

    suspend fun createCheckpoint(
        activeChatId: String,
        profileId: String?
    ): List<BranchInfo> {
        val current = sessionRepository.getSession(activeChatId) ?: return emptyList()
        val groupId: String
        val nextBranchIndex: Int

        if (current.checkpointGroupId == null) {
            groupId = UUID.randomUUID().toString()
            nextBranchIndex = FIRST_BRANCH_INDEX + 1
            sessionRepository.updateCheckpointFields(activeChatId, groupId, FIRST_BRANCH_INDEX)
        } else {
            groupId = current.checkpointGroupId ?: return emptyList()
            val siblings = sessionRepository.getSessionsByGroup(groupId)
            if (siblings.size >= MAX_BRANCHES) return emptyList()
            nextBranchIndex = siblings.maxOf { it.branchIndex } + 1
        }

        val now = System.currentTimeMillis()
        sessionRepository.upsertSession(
            ChatSession(
                id = UUID.randomUUID().toString(),
                messages = current.messages,
                createdAt = now,
                updatedAt = now,
                settingsJson = current.settingsJson,
                checkpointGroupId = groupId,
                branchIndex = nextBranchIndex,
                profileId = profileId
            )
        )

        currentGroupId = groupId
        return loadBranchesInternal(groupId)
    }

    suspend fun cleanupGroupOnExit(activeChatId: String, messagesEmpty: Boolean) {
        val groupId = currentGroupId ?: return
        if (messagesEmpty) {
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

    suspend fun clearSessionData(): ClearResult {
        val groupId = currentGroupId
        if (groupId != null) {
            val remaining = sessionRepository.getSessionsByGroup(groupId)
            if (remaining.size == 1) {
                val sole = remaining.first()
                sessionRepository.upsertSession(sole.copy(checkpointGroupId = null, branchIndex = 0))
                currentGroupId = null
                return ClearResult(newActiveChatId = sole.id, newMessages = sole.messages)
            }
            currentGroupId = null
        }
        return ClearResult(null, null)
    }

    private suspend fun loadBranchesInternal(groupId: String): List<BranchInfo> {
        currentGroupId = groupId
        val siblings = sessionRepository.getSessionsByGroup(groupId)
        if (siblings.size <= 1) {
            siblings.firstOrNull()?.let { session ->
                sessionRepository.upsertSession(session.copy(checkpointGroupId = null, branchIndex = 0))
            }
            currentGroupId = null
            return emptyList()
        }
        return siblings.map { BranchInfo(it.id, it.branchIndex) }
    }

    data class ClearResult(
        val newActiveChatId: String?,
        val newMessages: List<ChatMessage>?
    )
}
