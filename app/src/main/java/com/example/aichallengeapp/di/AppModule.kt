package com.example.aichallengeapp.di

import com.example.aichallengeapp.BuildConfig
import com.example.aichallengeapp.data.ApiKeyStorage
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single { ApiKeyStorage(get()) }

    single(named("apiKey")) {
        val storage = get<ApiKeyStorage>()
        resolveKey(storage::getApiKey, BuildConfig.DEEPSEEK_API_KEY, storage::saveApiKey)
    }

    single(named("baseUrl")) { BuildConfig.DEEPSEEK_BASE_URL }
    single(named("isDebug")) { BuildConfig.DEBUG }

    single(named("githubKey")) {
        val storage = get<ApiKeyStorage>()
        resolveKey(storage::getGithubKey, BuildConfig.GITHUB_KEY, storage::saveGithubKey)
    }

    single(named("mcpBaseUrl")) { BuildConfig.MCP_SERVER_URL }
}

private fun resolveKey(
    getStored: () -> String?,
    buildConfigKey: String,
    save: (String) -> Unit
): String {
    val stored = getStored()
    if (!stored.isNullOrEmpty()) return stored
    if (buildConfigKey.isNotEmpty()) save(buildConfigKey)
    return buildConfigKey
}
