package com.example.aichallengeapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String = DeepSeekDefaults.MODEL_CHAT,
    val messages: List<MessageDto>,
    @SerialName("max_tokens")
    val maxTokens: Int? = null
)

@Serializable
data class MessageDto(
    val role: String,
    val content: String
)

@Serializable
data class ChatResponse(
    val id: String,
    val choices: List<ChoiceDto>
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
    const val ROLE_SYSTEM = "system"
    const val ROLE_USER = "user"
    const val ROLE_ASSISTANT = "assistant"
    const val SYSTEM_PROMPT = "You are a helpful assistant"
}
