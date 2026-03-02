package com.example.aichallengeapp.feature.globalsettings.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GlobalSettings(val systemPromptPrefix: String = "")
