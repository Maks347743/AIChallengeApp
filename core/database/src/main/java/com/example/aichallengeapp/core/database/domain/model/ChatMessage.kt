package com.example.aichallengeapp.core.database.domain.model

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
    val id: String = generateId()
) {
    companion object {
        const val ROLE_SYSTEM = "system"
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val ROLE_SUMMARY = "summary"
        const val ROLE_FACTS = "facts"
        const val ROLE_CONSTRAINT_VIOLATION_ASSISTANT = "constraint_violation_assistant"
        const val ROLE_CONSTRAINT_VIOLATION_USER = "constraint_violation_user"
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun generateId(): String = Uuid.random().toString()
