package com.example.aichallengeapp.core.database.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.aichallengeapp.core.database.domain.model.ChatMessage

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val messages: List<ChatMessage>,
    val createdAt: Long,
    val updatedAt: Long,
    @ColumnInfo(name = "settings_json") val settingsJson: String? = null
)
