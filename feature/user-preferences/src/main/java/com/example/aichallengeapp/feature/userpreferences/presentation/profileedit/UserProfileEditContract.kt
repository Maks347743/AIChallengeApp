package com.example.aichallengeapp.feature.userpreferences.presentation.profileedit

import com.example.aichallengeapp.core.database.domain.model.Constraint

sealed interface UserProfileEditIntent {
    data class UpdateName(val name: String) : UserProfileEditIntent
    data class UpdateDescription(val description: String) : UserProfileEditIntent
    data object AddConstraint : UserProfileEditIntent
    data class RemoveConstraint(val index: Int) : UserProfileEditIntent
    data class UpdateConstraint(val index: Int, val constraint: Constraint) : UserProfileEditIntent
    data object Save : UserProfileEditIntent
}

data class UserProfileEditState(
    val name: String = "",
    val description: String = "",
    val constraints: List<Constraint> = emptyList()
)
