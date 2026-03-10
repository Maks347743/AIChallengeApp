package com.example.aichallengeapp.feature.chat.di

import com.example.aichallengeapp.core.database.domain.repository.ChatRepository
import com.example.aichallengeapp.feature.chat.data.mcp.McpToolClient
import com.example.aichallengeapp.feature.chat.data.repository.ChatRepositoryImpl
import com.example.aichallengeapp.feature.chat.domain.usecase.BuildSystemPromptUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.DetectNewTaskUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.DetectStageTransitionUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.ExecuteToolCallsUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.GenerateStageArtifactUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.GetToolDefinitionsUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.SendChatMessageUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.UpdateMetricsUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.ValidateConstraintsUseCase
import com.example.aichallengeapp.feature.chat.presentation.ChatViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import timber.log.Timber

private const val LOG_TAG = "KtorClient"

val chatModule = module {
    single {
        val isDebug: Boolean = get(named("isDebug"))
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 600_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 600_000
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = isDebug
                    encodeDefaults = true
                    explicitNulls = false
                })
            }
            if (isDebug) {
                install(Logging) {
                    level = LogLevel.BODY
                    logger = object : Logger {
                        override fun log(message: String) {
                            message.chunked(3000).forEach { Timber.tag(LOG_TAG).d(it) }
                        }
                    }
                }
            }
        }
    }

    single<ChatRepository> {
        ChatRepositoryImpl(
            httpClient = get(),
            apiKey = get(named("apiKey")),
            baseUrl = get(named("baseUrl"))
        )
    }

    single {
        val mcpJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
        }
        McpToolClient(
            httpClient = get(named("mcpClient")),
            mcpBaseUrl = get(named("mcpBaseUrl")),
            json = mcpJson
        )
    }

    factory { SendChatMessageUseCase(get()) }
    factory { DetectStageTransitionUseCase(get()) }
    factory { DetectNewTaskUseCase(get()) }
    factory { GenerateStageArtifactUseCase(get()) }
    factory { ValidateConstraintsUseCase() }
    factory { BuildSystemPromptUseCase() }
    factory { UpdateMetricsUseCase(get()) }
    factory {
        ExecuteToolCallsUseCase(
            mcpToolClient = get(),
            json = Json { ignoreUnknownKeys = true }
        )
    }
    factory { GetToolDefinitionsUseCase(get()) }

    viewModel { params ->
        ChatViewModel(
            chatId = params.get(),
            initialBranchIndex = params.get(),
            initialProfileId = params.get(),
            sendChatMessageUseCase = get(),
            detectStageTransitionUseCase = get(),
            detectNewTaskUseCase = get(),
            generateStageArtifactUseCase = get(),
            validateConstraintsUseCase = get(),
            buildSystemPromptUseCase = get(),
            updateMetricsUseCase = get(),
            executeToolCallsUseCase = get(),
            getToolDefinitionsUseCase = get(),
            settingsRepository = get(),
            sessionRepository = get(),
            metricsRepository = get(),
            userProfileRepository = get()
        )
    }
}
