package com.example.aichallengeapp.core.database.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ChatSessionEntity::class,
        ChatMetricsEntity::class,
        UserProfileEntity::class,
        PeriodicTaskEntity::class,
        PeriodicTaskResultEntity::class
    ],
    version = 12,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMetricsDao(): ChatMetricsDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun periodicTaskDao(): PeriodicTaskDao
}
