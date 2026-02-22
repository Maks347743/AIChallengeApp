package com.example.aichallengeapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String = DeepSeekDefaults.MODEL_CHAT,
    val messages: List<MessageDto>,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    val temperature: Float? = null
)

@Serializable
data class MessageDto(
    val role: String,
    val content: String
)

@Serializable
data class UsageDto(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0
)

@Serializable
data class ChatResponse(
    val id: String,
    val choices: List<ChoiceDto>,
    val usage: UsageDto? = null
)

@Serializable
data class ChoiceDto(
    val index: Int,
    val message: MessageDto,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

object DeepSeekDefaults {
    const val MODEL_CHAT = "deepseek-chat"
}
