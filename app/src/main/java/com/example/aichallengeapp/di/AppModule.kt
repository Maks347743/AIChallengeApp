package com.example.aichallengeapp.di

import com.example.aichallengeapp.BuildConfig
import com.example.aichallengeapp.data.ApiKeyStorage
import com.example.aichallengeapp.feature.chat.data.tools.CreatePeriodicTaskTool
import com.example.aichallengeapp.feature.chat.data.tools.ListPeriodicTasksTool
import com.example.aichallengeapp.feature.chat.data.tools.LocalToolRegistry
import com.example.aichallengeapp.feature.chat.data.tools.RunPipelineTool
import com.example.aichallengeapp.feature.chat.data.tools.StopPeriodicTaskTool
import com.example.aichallengeapp.service.PeriodicTaskExecutor
import com.example.aichallengeapp.service.PeriodicTaskServiceControllerImpl
import com.example.aichallengeapp.core.periodictask.domain.PeriodicTaskManager
import com.example.aichallengeapp.core.periodictask.domain.PeriodicTaskServiceController
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
    single(named("deepwikiMcpUrl")) { BuildConfig.DEEPWIKI_MCP_URL }
    single(named("ragMcpUrl")) { BuildConfig.RAG_MCP_URL }
    single<PeriodicTaskServiceController> { PeriodicTaskServiceControllerImpl(get()) }
    single { PeriodicTaskManager(get(), get()) }

    single {
        PeriodicTaskExecutor(
            mcpToolClientManager = get(),
            periodicTaskRepository = get(),
            chatRepository = get(),
            json = get(named("appJson")),
            settingsRepository = get()
        )
    }

    single<LocalToolRegistry> {
        LocalToolRegistry(
            listOf(
                CreatePeriodicTaskTool(get(), get()),
                StopPeriodicTaskTool(get(), get()),
                ListPeriodicTasksTool(get()),
                RunPipelineTool(
                    mcpToolClientManager = get(),
                    chatRepository = get(),
                    settingsRepository = get()
                )
            )
        )
    }
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
