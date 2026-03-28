package com.example.ragserver.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Shared data classes for OpenAI-compatible chat completions API (used by DeepSeek). */

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class ChatChoice(val message: ChatMessage)

@Serializable
data class ChatResponse(val choices: List<ChatChoice> = emptyList())

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("max_tokens") val maxTokens: Int
)

/** Native Ollama /api/chat request — supports think:false to suppress reasoning blocks. */
@Serializable
data class OllamaChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    val think: Boolean = false,
    @SerialName("keep_alive") val keepAlive: Int = -1
)

/** Native Ollama /api/chat response. */
@Serializable
data class OllamaChatResponse(
    val message: ChatMessage = ChatMessage("assistant", "")
)
