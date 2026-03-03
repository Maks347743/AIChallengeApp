package com.example.aichallengeapp.feature.userpreferences.presentation.profilelist

import com.example.aichallengeapp.core.database.domain.model.UserProfile

sealed interface UserProfileListIntent {
    data class DeleteProfile(val id: String) : UserProfileListIntent
}

data class UserProfileListState(
    val profiles: List<UserProfile> = emptyList()
)
