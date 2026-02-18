package com.example.aichallengeapp.di

import android.content.Context
import com.example.aichallengeapp.BuildConfig
import com.example.aichallengeapp.data.SettingsStorage
import com.example.aichallengeapp.data.repository.ChatRepositoryImpl
import com.example.aichallengeapp.domain.repository.ChatRepository
import com.example.aichallengeapp.domain.usecase.SendChatMessageUseCase
import com.example.aichallengeapp.presentation.HomeViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import android.util.Log
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private const val LOG_TAG = "KtorClient"

val appModule = module {

    // Network
    single {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 60_000
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
                        Log.d(LOG_TAG, message)
                    }
                }
            }
        }
    }

    // Data
    single {
        SettingsStorage(get<Context>().getSharedPreferences("chat_settings", Context.MODE_PRIVATE))
    }

    single<ChatRepository> {
        ChatRepositoryImpl(
            httpClient = get(),
            apiKey = BuildConfig.DEEPSEEK_API_KEY,
            baseUrl = BuildConfig.DEEPSEEK_BASE_URL
        )
    }

    // Domain
    factory { SendChatMessageUseCase(chatRepository = get()) }

    // Presentation
    viewModel { HomeViewModel(sendChatMessageUseCase = get(), settingsStorage = get()) }
}
