package com.example.aichallengeapp.core.database.domain.model

import kotlinx.serialization.Serializable

data class ChatResult(
    val message: String,
    val metrics: ResponseMetrics,
    val toolCalls: List<ToolCallInfo>? = null,
    val finishReason: String? = null
) {
    companion object {
        const val FINISH_REASON_TOOL_CALLS = "tool_calls"
    }
}

@Serializable
data class ToolCallInfo(
    val id: String,
    val functionName: String,
    val arguments: String
)
