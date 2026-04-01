package com.example.aichallengeapp.feature.chat.di

import com.example.aichallengeapp.core.database.domain.repository.ChatRepository
import com.example.aichallengeapp.core.mcp.HomeServerConfig
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
        val homeServerConfig: HomeServerConfig = get()
        HttpClient(OkHttp) {
            engine {
                addInterceptor { chain ->
                    val key = homeServerConfig.apiKey
                    val requestBuilder = chain.request().newBuilder()
                    if (key.isNotEmpty()) requestBuilder.addHeader("X-API-Key", key)
                    chain.proceed(requestBuilder.build())
                }
            }
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

    single { HomeServerConfig() }

    single(named("githubMcpClient")) {
        val config = get<HomeServerConfig>()
        val fallbackUrl: String = get(named("mcpBaseUrl"))
        McpToolClient(
            httpClient = get(),
            mcpBaseUrlProvider = {
                config.baseUrl.ifEmpty { fallbackUrl }.let { base ->
                    if (config.baseUrl.isEmpty()) base else "$base/github-mcp/mcp"
                }
            },
            json = get(named("appJson"))
        )
    }

    single(named("deepwikiMcpClient")) {
        McpToolClient(
            httpClient = get(),
            mcpBaseUrlProvider = { get(named("deepwikiMcpUrl")) },
            json = get(named("appJson"))
        )
    }

    single(named("ragMcpClient")) {
        val config = get<HomeServerConfig>()
        val fallbackUrl: String = get(named("ragMcpUrl"))
        McpToolClient(
            httpClient = get(),
            mcpBaseUrlProvider = {
                config.baseUrl.ifEmpty { fallbackUrl }.let { base ->
                    if (config.baseUrl.isEmpty()) base else "$base/rag/mcp"
                }
            },
            json = get(named("appJson"))
        )
    }

    single(named("supportMcpClient")) {
        McpToolClient(
            httpClient = get(),
            mcpBaseUrlProvider = { get(named("supportMcpUrl")) },
            json = get(named("appJson"))
        )
    }

    single {
        McpToolClientManager(
            listOf(
                McpServerConfig("GitHub MCP", get(named("githubMcpClient"))),
                McpServerConfig("DeepWiki", get(named("deepwikiMcpClient"))),
                McpServerConfig("RAG", get(named("ragMcpClient"))),
                McpServerConfig("Support", get(named("supportMcpClient")))
            )
        )
    }

    factory { SendChatMessageUseCase(get()) }
    factory { ValidateConstraintsUseCase() }
    factory { BuildSystemPromptUseCase() }
    factory { UpdateMetricsUseCase(get()) }
    factory { UpdateTaskMemoryUseCase(get<SendChatMessageUseCase>()) }
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
            appSettingsRepository = get(),
            homeServerConfig = get(),
            sessionManager = get(),
            metricsRepository = get(),
            userProfileRepository = get(),
            periodicTaskMessageBus = get()
        )
    }
}
