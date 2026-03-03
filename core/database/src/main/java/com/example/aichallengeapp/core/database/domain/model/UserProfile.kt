package com.example.aichallengeapp.core.database.domain.model

data class UserProfile(
    val id: String,
    val name: String,
    val description: String,
    val createdAt: Long
)
