package com.example.aichallengeapp.core.database.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMetricsDao {

    @Query("SELECT * FROM chat_metrics WHERE chatId = :chatId")
    fun observe(chatId: String): Flow<ChatMetricsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ChatMetricsEntity)

    @Query("DELETE FROM chat_metrics WHERE chatId = :chatId")
    suspend fun delete(chatId: String)
}
