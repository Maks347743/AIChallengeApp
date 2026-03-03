package com.example.aichallengeapp.feature.userpreferences.di

import com.example.aichallengeapp.feature.userpreferences.presentation.profileedit.UserProfileEditViewModel
import com.example.aichallengeapp.feature.userpreferences.presentation.profilelist.UserProfileListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val userPreferencesModule = module {
    viewModel { UserProfileListViewModel(get()) }
    viewModel { params -> UserProfileEditViewModel(params.getOrNull(), get()) }
}
