package com.example.aichallengeapp.core.database.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

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
        const val ROLE_TOOL_CALL = "tool_call"
        const val ROLE_TOOL_RESULT = "tool_result"
    }
}

private fun generateId(): String = UUID.randomUUID().toString()
