package com.example.aichallengeapp.core.database.di

import android.content.Context
import androidx.room.Room
import com.example.aichallengeapp.core.database.data.db.AppDatabase
import com.example.aichallengeapp.core.database.data.repository.ChatMetricsRepositoryImpl
import com.example.aichallengeapp.core.database.data.repository.ChatSessionRepositoryImpl
import com.example.aichallengeapp.core.database.data.repository.PeriodicTaskRepositoryImpl
import com.example.aichallengeapp.core.database.data.repository.UserProfileRepositoryImpl
import com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository
import com.example.aichallengeapp.core.database.domain.repository.ChatSessionRepository
import com.example.aichallengeapp.core.periodictask.domain.repository.PeriodicTaskRepository
import com.example.aichallengeapp.core.periodictask.domain.model.PeriodicTaskMessageBus
import com.example.aichallengeapp.core.database.domain.repository.UserProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(get<Context>(), AppDatabase::class.java, "app_db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
    single { get<AppDatabase>().chatSessionDao() }
    single { get<AppDatabase>().chatMetricsDao() }
    single { get<AppDatabase>().userProfileDao() }
    single { get<AppDatabase>().periodicTaskDao() }
    single<ChatSessionRepository> { ChatSessionRepositoryImpl(get()) }
    single<ChatMetricsRepository> { ChatMetricsRepositoryImpl(get()) }
    single<UserProfileRepository> { UserProfileRepositoryImpl(get(), get(named("appJson"))) }
    single<PeriodicTaskRepository> { PeriodicTaskRepositoryImpl(get()) }
    single { PeriodicTaskMessageBus() }
    single(named("appJson")) {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
        }
    }
}
