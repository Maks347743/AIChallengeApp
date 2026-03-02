package com.example.aichallengeapp.feature.globalsettings.di

import com.example.aichallengeapp.feature.globalsettings.data.GlobalSettingsRepositoryImpl
import com.example.aichallengeapp.feature.globalsettings.domain.repository.GlobalSettingsRepository
import com.example.aichallengeapp.feature.globalsettings.presentation.GlobalSettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val globalSettingsModule = module {
    single<GlobalSettingsRepository> { GlobalSettingsRepositoryImpl(get()) }
    viewModel { GlobalSettingsViewModel(get()) }
}
