package com.example.aichallengeapp

import android.app.Application
import com.example.aichallengeapp.core.database.di.databaseModule
import com.example.aichallengeapp.di.appModule
import com.example.aichallengeapp.feature.chat.di.chatModule
import com.example.aichallengeapp.feature.chatlist.di.chatListModule
import com.example.aichallengeapp.feature.settings.di.settingsModule
import com.example.aichallengeapp.feature.explore.di.exploreGitHubModule
import com.example.aichallengeapp.feature.userpreferences.di.userPreferencesModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule, databaseModule, settingsModule, chatModule, chatListModule, userPreferencesModule, exploreGitHubModule)
        }
    }
}
