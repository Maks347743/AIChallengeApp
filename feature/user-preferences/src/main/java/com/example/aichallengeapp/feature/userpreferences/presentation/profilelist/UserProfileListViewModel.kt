package com.example.aichallengeapp.feature.userpreferences.presentation.profilelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichallengeapp.core.database.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserProfileListViewModel(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    val state: StateFlow<UserProfileListState> = userProfileRepository.getAllProfiles()
        .map { profiles -> UserProfileListState(profiles = profiles) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UserProfileListState())

    fun onIntent(intent: UserProfileListIntent) {
        when (intent) {
            is UserProfileListIntent.DeleteProfile -> viewModelScope.launch {
                userProfileRepository.delete(intent.id)
            }
        }
    }
}
