package com.example.aichallengeapp.core.database.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "periodic_task_results",
    foreignKeys = [
        ForeignKey(
            entity = PeriodicTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PeriodicTaskResultEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "task_id", index = true) val taskId: String,
    @ColumnInfo(name = "result") val result: String,
    @ColumnInfo(name = "summary") val summary: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
