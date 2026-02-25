package com.example.aichallengeapp.core.database.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_metrics",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ]
)
data class ChatMetricsEntity(
    @PrimaryKey val chatId: String,
    val lastRequestTokens: Int,
    val lastResponseTokens: Int,
    val totalTokens: Int
)
