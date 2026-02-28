package com.example.aichallengeapp.feature.settings.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class DeepSeekModel(val id: String, val displayName: String) {
    DEEPSEEK_CHAT("deepseek-chat", "DeepSeek Chat"),
    DEEPSEEK_REASONER("deepseek-reasoner", "DeepSeek Reasoner")
}
