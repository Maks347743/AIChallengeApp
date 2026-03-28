package com.example.aichallengeapp.feature.settings.di

import com.example.aichallengeapp.feature.settings.data.AppSettingsRepositoryImpl
import com.example.aichallengeapp.feature.settings.data.ChatSettingsRepositoryImpl
import com.example.aichallengeapp.feature.settings.domain.repository.AppSettingsRepository
import com.example.aichallengeapp.feature.settings.domain.repository.ChatSettingsRepository
import com.example.aichallengeapp.feature.settings.presentation.SettingsViewModel
import com.example.aichallengeapp.feature.settings.presentation.appsettings.AppSettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val settingsModule = module {
    single<ChatSettingsRepository> {
        ChatSettingsRepositoryImpl(
            sessionRepository = get(),
            json = get(named("appJson"))
        )
    }
    single<AppSettingsRepository> {
        AppSettingsRepositoryImpl(
            context = androidContext(),
            json = get(named("appJson"))
        )
    }
    viewModel { params -> SettingsViewModel(params.get(), get()) }
    viewModel { AppSettingsViewModel(get()) }
}
