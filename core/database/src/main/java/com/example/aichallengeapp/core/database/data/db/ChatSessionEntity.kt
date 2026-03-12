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
    @ColumnInfo(name = "settings_json") val settingsJson: String? = null,
    @ColumnInfo(name = "checkpoint_group_id") val checkpointGroupId: String? = null,
    @ColumnInfo(name = "branch_index") val branchIndex: Int = 0,
    @ColumnInfo(name = "profile_id") val profileId: String? = null,
    @ColumnInfo(name = "is_periodic_task", defaultValue = "0") val isPeriodicTask: Boolean = false
)
