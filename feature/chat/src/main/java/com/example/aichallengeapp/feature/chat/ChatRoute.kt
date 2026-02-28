package com.example.aichallengeapp.feature.chat

import kotlinx.serialization.Serializable

@Serializable
data class ChatRoute(val chatId: String, val branchIndex: Int = 0)
