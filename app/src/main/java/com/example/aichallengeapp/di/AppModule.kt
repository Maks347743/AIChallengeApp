package com.example.aichallengeapp.di

import com.example.aichallengeapp.BuildConfig
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single(named("apiKey")) { BuildConfig.DEEPSEEK_API_KEY }
    single(named("baseUrl")) { BuildConfig.DEEPSEEK_BASE_URL }
}
