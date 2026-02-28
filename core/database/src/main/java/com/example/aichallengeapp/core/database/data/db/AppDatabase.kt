package com.example.aichallengeapp.core.database.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ChatSessionEntity::class, ChatMetricsEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMetricsDao(): ChatMetricsDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chat_metrics` (
                `chatId` TEXT NOT NULL,
                `lastRequestTokens` INTEGER NOT NULL,
                `lastResponseTokens` INTEGER NOT NULL,
                `totalTokens` INTEGER NOT NULL,
                PRIMARY KEY(`chatId`),
                FOREIGN KEY(`chatId`) REFERENCES `chat_sessions`(`id`)
                ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `chat_sessions` ADD COLUMN `settings_json` TEXT")
    }
}
