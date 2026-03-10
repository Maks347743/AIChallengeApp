package com.example.aichallengeapp.core.database.domain.model

import kotlinx.serialization.Serializable

data class ChatResult(
    val message: String,
    val metrics: ResponseMetrics,
    val toolCalls: List<ToolCallInfo>? = null,
    val finishReason: String? = null
)

@Serializable
data class ToolCallInfo(
    val id: String,
    val functionName: String,
    val arguments: String
)
