package com.example.aichallengeapp.feature.settings.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val model: DeepSeekModel = DeepSeekModel.DEEPSEEK_CHAT,
    val ollamaModelName: String = "qwen3:4b",
    val serverBaseUrl: String = "",
    val mcpServerToken: String = "",
) {
    val ollamaBaseUrl: String
        get() = if (serverBaseUrl.isNotEmpty()) "$serverBaseUrl/ollama/v1" else "http://10.0.2.2:11434/v1"
}
