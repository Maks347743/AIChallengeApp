package com.example.aichallengeapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.aichallengeapp.core.database.di.databaseModule
import com.example.aichallengeapp.core.periodictask.domain.PeriodicTaskManager
import com.example.aichallengeapp.core.periodictask.domain.model.PeriodicTaskConstants
import com.example.aichallengeapp.di.appModule
import com.example.aichallengeapp.feature.chat.di.chatModule
import com.example.aichallengeapp.feature.chatlist.di.chatListModule
import com.example.aichallengeapp.feature.settings.di.settingsModule
import com.example.aichallengeapp.feature.userpreferences.di.userPreferencesModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class MainApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule, databaseModule, settingsModule, chatModule, chatListModule, userPreferencesModule)
        }

        createPeriodicTaskNotificationChannel()
        resumePeriodicTasks()
    }

    private fun createPeriodicTaskNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PeriodicTaskConstants.NOTIFICATION_CHANNEL_ID,
                "Periodic Tasks",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when periodic tasks are running"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun resumePeriodicTasks() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                val manager: PeriodicTaskManager by inject()
                manager.resumeIfNeeded()
            } catch (e: Exception) {
                Timber.tag("MainApplication").w(e, "Failed to resume periodic tasks")
            }
        }
    }
}
