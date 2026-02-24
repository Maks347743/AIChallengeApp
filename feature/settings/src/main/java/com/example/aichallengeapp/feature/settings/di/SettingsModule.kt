package com.example.aichallengeapp.feature.settings.di

import android.content.Context
import com.example.aichallengeapp.feature.settings.data.SettingsStorage
import com.example.aichallengeapp.feature.settings.domain.repository.SettingsRepository
import com.example.aichallengeapp.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    single<SettingsRepository> {
        SettingsStorage(get<Context>().getSharedPreferences("chat_settings", Context.MODE_PRIVATE))
    }
    viewModel { SettingsViewModel(get()) }
}
