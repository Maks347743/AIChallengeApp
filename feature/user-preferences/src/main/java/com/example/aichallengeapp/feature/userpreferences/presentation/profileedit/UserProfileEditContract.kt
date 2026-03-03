package com.example.aichallengeapp.feature.userpreferences.presentation.profileedit

sealed interface UserProfileEditIntent {
    data class UpdateName(val name: String) : UserProfileEditIntent
    data class UpdateDescription(val description: String) : UserProfileEditIntent
    data object Save : UserProfileEditIntent
}

data class UserProfileEditState(
    val name: String = "",
    val description: String = ""
)
