package com.example.aichallengeapp.feature.settings.di

import com.example.aichallengeapp.feature.settings.data.ChatSettingsRepositoryImpl
import com.example.aichallengeapp.feature.settings.domain.repository.ChatSettingsRepository
import com.example.aichallengeapp.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    single<ChatSettingsRepository> { ChatSettingsRepositoryImpl(get()) }
    viewModel { params -> SettingsViewModel(params.get(), get()) }
}
