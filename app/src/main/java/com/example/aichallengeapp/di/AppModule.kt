package com.example.aichallengeapp.di

import com.example.aichallengeapp.BuildConfig
import com.example.aichallengeapp.data.ApiKeyStorage
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single { ApiKeyStorage(get()) }

    single(named("apiKey")) {
        val storage = get<ApiKeyStorage>()
        val storedKey = storage.getApiKey()
        if (!storedKey.isNullOrEmpty()) {
            storedKey
        } else {
            val buildConfigKey = BuildConfig.DEEPSEEK_API_KEY
            if (buildConfigKey.isNotEmpty()) {
                storage.saveApiKey(buildConfigKey)
            }
            buildConfigKey
        }
    }

    single(named("baseUrl")) { BuildConfig.DEEPSEEK_BASE_URL }
    single(named("isDebug")) { BuildConfig.DEBUG }

    single(named("githubKey")) {
        val storage = get<ApiKeyStorage>()
        val storedKey = storage.getGithubKey()
        if (!storedKey.isNullOrEmpty()) {
            storedKey
        } else {
            val buildConfigKey = BuildConfig.GITHUB_KEY
            if (buildConfigKey.isNotEmpty()) {
                storage.saveGithubKey(buildConfigKey)
            }
            buildConfigKey
        }
    }

    single(named("mcpBaseUrl")) { BuildConfig.GITHUB_MCP_BASE_URL }
}
