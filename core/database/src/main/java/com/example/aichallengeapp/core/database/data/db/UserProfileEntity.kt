package com.example.aichallengeapp.core.database.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val createdAt: Long
)
