package com.example.aichallengeapp.core.database.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "periodic_tasks")
data class PeriodicTaskEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "chat_id") val chatId: String,
    @ColumnInfo(name = "tool_name") val toolName: String,
    @ColumnInfo(name = "tool_arguments_json") val toolArgumentsJson: String,
    @ColumnInfo(name = "interval_minutes") val intervalMinutes: Int,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "last_executed_at") val lastExecutedAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "prompt") val prompt: String
)
