package com.example.aichallengeapp.core.database.domain.repository

import com.example.aichallengeapp.core.database.domain.model.ChatMessage
import com.example.aichallengeapp.core.database.domain.model.ChatResult
import com.example.aichallengeapp.core.mcp.model.ToolDefinition

interface ChatRepository {
    suspend fun sendMessage(
        messages: List<ChatMessage>,
        maxTokens: Int?,
        temperature: Float?,
        model: String,
        tools: List<ToolDefinition>? = null,
        baseUrlOverride: String? = null,
        apiKeyOverride: String? = null,
        think: Boolean? = null
    ): Result<ChatResult>

    suspend fun sendRawMessage(prompt: String): String
}
