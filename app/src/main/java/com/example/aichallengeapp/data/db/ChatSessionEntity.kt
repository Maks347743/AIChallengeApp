package com.example.aichallengeapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.aichallengeapp.domain.model.ChatMessage

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val messages: List<ChatMessage>,
    val createdAt: Long,
    val updatedAt: Long
)
