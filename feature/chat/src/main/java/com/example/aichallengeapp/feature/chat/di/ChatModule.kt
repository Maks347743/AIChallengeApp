package com.example.aichallengeapp.feature.chat.di

import android.util.Log
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

private const val LOG_TAG = "KtorClient"

val chatModule = module {
    single {
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
                    prettyPrint = true
                    encodeDefaults = true
                })
            }
            install(Logging) {
                level = LogLevel.BODY
                logger = object : Logger {
                    override fun log(message: String) {
                        message.chunked(3000).forEach { Log.d(LOG_TAG, it) }
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

    viewModel { params -> ChatViewModel(params.get(), params.get(), get(), get(), get(), get(), get()) }
}
