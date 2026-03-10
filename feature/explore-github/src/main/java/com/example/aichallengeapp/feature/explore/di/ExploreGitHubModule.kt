package com.example.aichallengeapp.feature.explore.di

import com.example.aichallengeapp.feature.explore.data.repository.GitHubMcpRepositoryImpl
import com.example.aichallengeapp.feature.explore.presentation.ExploreGitHubViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import timber.log.Timber

val exploreGitHubModule = module {
    single(named("mcpJson")) {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
            isLenient = true
        }
    }

    single(named("mcpClient")) {
        val isDebug: Boolean = get(named("isDebug"))
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(get(named("mcpJson")))
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }
            if (isDebug) {
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            Timber.tag("McpHttp").d(message)
                        }
                    }
                    level = LogLevel.BODY
                }
            }
        }
    }

    single<com.example.aichallengeapp.feature.explore.domain.GitHubMcpRepository> {
        GitHubMcpRepositoryImpl(
            httpClient = get(named("mcpClient")),
            mcpBaseUrl = get(named("mcpBaseUrl")),
            json = get(named("mcpJson"))
        )
    }

    viewModel { ExploreGitHubViewModel(get()) }
}
