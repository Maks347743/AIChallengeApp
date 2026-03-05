package com.example.aichallengeapp.core.database.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Constraint(
    val name: String,
    val description: String,
    val regexPattern: String,
    val matchMeansViolation: Boolean = false
)
