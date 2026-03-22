package com.example.aichallengeapp.feature.chat.di

import com.example.aichallengeapp.core.database.domain.repository.ChatRepository
import com.example.aichallengeapp.feature.chat.data.mcp.McpServerConfig
import com.example.aichallengeapp.feature.chat.data.mcp.McpToolClient
import com.example.aichallengeapp.feature.chat.data.mcp.McpToolClientManager
import com.example.aichallengeapp.feature.chat.data.repository.ChatRepositoryImpl
import com.example.aichallengeapp.feature.chat.domain.ChatSessionManager
import com.example.aichallengeapp.feature.chat.domain.usecase.BuildSystemPromptUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.ExecuteToolCallsUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.GetToolDefinitionsUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.SendChatMessageUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.UpdateMetricsUseCase
import com.example.aichallengeapp.feature.chat.domain.usecase.UpdateTaskMemoryUseCase
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
            baseUrl = get(named("baseUrl")),
            json = get(named("appJson"))
        )
    }

    single(named("githubMcpClient")) {
        McpToolClient(
            httpClient = get(named("mcpClient")),
            mcpBaseUrl = get(named("mcpBaseUrl")),
            json = get(named("appJson"))
        )
    }

    single(named("deepwikiMcpClient")) {
        McpToolClient(
            httpClient = get(named("mcpClient")),
            mcpBaseUrl = get(named("deepwikiMcpUrl")),
            json = get(named("appJson"))
        )
    }

    single(named("ragMcpClient")) {
        McpToolClient(
            httpClient = get(named("mcpClient")),
            mcpBaseUrl = get(named("ragMcpUrl")),
            json = get(named("appJson"))
        )
    }

    single {
        McpToolClientManager(
            listOf(
                McpServerConfig("GitHub MCP", get(named("githubMcpClient"))),
                McpServerConfig("DeepWiki", get(named("deepwikiMcpClient"))),
                McpServerConfig("RAG", get(named("ragMcpClient")))
            )
        )
    }

    factory { SendChatMessageUseCase(get()) }
    factory { ValidateConstraintsUseCase() }
    factory { BuildSystemPromptUseCase() }
    factory { UpdateMetricsUseCase(get()) }
    factory { UpdateTaskMemoryUseCase(get()) }
    factory {
        ExecuteToolCallsUseCase(
            mcpToolClientManager = get(),
            localToolRegistry = get(),
            json = get(named("appJson"))
        )
    }
    factory { GetToolDefinitionsUseCase(get(), get()) }
    factory { ChatSessionManager(get()) }

    viewModel { params ->
        ChatViewModel(
            chatId = params.get(),
            initialBranchIndex = params.get(),
            initialProfileId = params.get(),
            sendChatMessageUseCase = get(),
            validateConstraintsUseCase = get(),
            buildSystemPromptUseCase = get(),
            updateMetricsUseCase = get(),
            updateTaskMemoryUseCase = get(),
            executeToolCallsUseCase = get(),
            getToolDefinitionsUseCase = get(),
            settingsRepository = get(),
            sessionManager = get(),
            metricsRepository = get(),
            userProfileRepository = get(),
            periodicTaskMessageBus = get()
        )
    }
}
