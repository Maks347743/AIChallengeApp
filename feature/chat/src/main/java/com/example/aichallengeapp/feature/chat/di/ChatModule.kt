package com.example.aichallengeapp.feature.chat.di

import com.example.aichallengeapp.core.database.domain.repository.ChatRepository
import com.example.aichallengeapp.feature.chat.data.repository.ChatRepositoryImpl
import com.example.aichallengeapp.feature.chat.domain.usecase.SendChatMessageUseCase
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

    factory { SendChatMessageUseCase(get()) }

    viewModel { params -> ChatViewModel(params.get(), params.get(), params.get(), get(), get(), get(), get(), get()) }
}
