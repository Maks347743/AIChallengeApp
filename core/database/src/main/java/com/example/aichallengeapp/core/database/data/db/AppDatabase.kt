package com.example.aichallengeapp.core.database.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ChatSessionEntity::class, ChatMetricsEntity::class, UserProfileEntity::class],
    version = 9,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMetricsDao(): ChatMetricsDao
    abstract fun userProfileDao(): UserProfileDao
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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `chat_sessions` ADD COLUMN `checkpoint_group_id` TEXT")
        db.execSQL("ALTER TABLE `chat_sessions` ADD COLUMN `branch_index` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `chat_sessions` ADD COLUMN `current_task` TEXT")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `chat_sessions` ADD COLUMN `current_task_stage` TEXT")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `chat_sessions` ADD COLUMN `stage_artifacts_json` TEXT")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `user_profiles` ADD COLUMN `constraints_json` TEXT")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `user_profiles` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("ALTER TABLE `chat_sessions` ADD COLUMN `profile_id` TEXT")
    }
}
