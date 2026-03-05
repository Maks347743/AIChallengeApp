package com.example.aichallengeapp.feature.userpreferences.presentation.profileedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichallengeapp.core.database.domain.model.Constraint
import com.example.aichallengeapp.core.database.domain.model.UserProfile
import com.example.aichallengeapp.core.database.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class UserProfileEditViewModel(
    private val profileId: String?,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(UserProfileEditState())
    val state: StateFlow<UserProfileEditState> = _state.asStateFlow()

    private var existingId: String? = null

    init {
        if (profileId != null) {
            viewModelScope.launch {
                val profile = userProfileRepository.getById(profileId)
                if (profile != null) {
                    existingId = profile.id
                    _state.update { it.copy(name = profile.name, description = profile.description, constraints = profile.constraints) }
                }
            }
        }
    }

    fun onIntent(intent: UserProfileEditIntent) {
        when (intent) {
            is UserProfileEditIntent.UpdateName -> _state.update { it.copy(name = intent.name) }
            is UserProfileEditIntent.UpdateDescription -> _state.update { it.copy(description = intent.description) }
            is UserProfileEditIntent.AddConstraint -> _state.update {
                it.copy(constraints = it.constraints + Constraint(name = "", description = "", regexPattern = ""))
            }
            is UserProfileEditIntent.RemoveConstraint -> _state.update {
                it.copy(constraints = it.constraints.toMutableList().apply { removeAt(intent.index) })
            }
            is UserProfileEditIntent.UpdateConstraint -> _state.update {
                it.copy(constraints = it.constraints.toMutableList().apply { set(intent.index, intent.constraint) })
            }
            is UserProfileEditIntent.Save -> save()
        }
    }

    private fun save() {
        val current = _state.value
        val id = existingId ?: UUID.randomUUID().toString()
        viewModelScope.launch {
            userProfileRepository.upsert(
                UserProfile(
                    id = id,
                    name = current.name.trim(),
                    description = current.description.trim(),
                    createdAt = if (existingId != null) {
                        userProfileRepository.getById(id)?.createdAt ?: System.currentTimeMillis()
                    } else {
                        System.currentTimeMillis()
                    },
                    constraints = current.constraints.filter { it.name.isNotBlank() || it.regexPattern.isNotBlank() }
                )
            )
        }
    }
}
