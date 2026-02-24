package com.example.aichallengeapp

import android.app.Application
import com.example.aichallengeapp.core.database.di.databaseModule
import com.example.aichallengeapp.di.appModule
import com.example.aichallengeapp.feature.chat.di.chatModule
import com.example.aichallengeapp.feature.chatlist.di.chatListModule
import com.example.aichallengeapp.feature.settings.di.settingsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule, databaseModule, settingsModule, chatModule, chatListModule)
        }
    }
}
