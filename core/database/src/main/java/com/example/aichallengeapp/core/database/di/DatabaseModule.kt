package com.example.aichallengeapp.core.database.di

import android.content.Context
import androidx.room.Room
import com.example.aichallengeapp.core.database.data.db.AppDatabase
import com.example.aichallengeapp.core.database.data.db.MIGRATION_1_2
import com.example.aichallengeapp.core.database.data.repository.ChatMetricsRepositoryImpl
import com.example.aichallengeapp.core.database.data.repository.ChatSessionRepositoryImpl
import com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository
import com.example.aichallengeapp.core.database.domain.repository.ChatSessionRepository
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(get<Context>(), AppDatabase::class.java, "app_db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }
    single { get<AppDatabase>().chatSessionDao() }
    single { get<AppDatabase>().chatMetricsDao() }
    single<ChatSessionRepository> { ChatSessionRepositoryImpl(get()) }
    single<ChatMetricsRepository> { ChatMetricsRepositoryImpl(get()) }
}
