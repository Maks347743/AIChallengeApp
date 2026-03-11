package com.example.aichallengeapp.core.database.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodicTaskDao {

    @Query("SELECT * FROM periodic_tasks WHERE is_active = 1")
    fun getAllActive(): Flow<List<PeriodicTaskEntity>>

    @Query("SELECT * FROM periodic_tasks WHERE is_active = 1")
    suspend fun getAllActiveSuspend(): List<PeriodicTaskEntity>

    @Query("SELECT * FROM periodic_tasks WHERE chat_id = :chatId")
    suspend fun getByChatId(chatId: String): List<PeriodicTaskEntity>

    @Query("SELECT * FROM periodic_tasks WHERE id = :id")
    suspend fun getById(id: String): PeriodicTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: PeriodicTaskEntity)

    @Query("UPDATE periodic_tasks SET is_active = 0 WHERE id = :taskId")
    suspend fun deactivate(taskId: String)

    @Query("UPDATE periodic_tasks SET is_active = 0 WHERE chat_id = :chatId")
    suspend fun deactivateByChatId(chatId: String)

    @Query("UPDATE periodic_tasks SET last_executed_at = :timestamp WHERE id = :taskId")
    suspend fun updateLastExecuted(taskId: String, timestamp: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: PeriodicTaskResultEntity)

    @Query("SELECT * FROM periodic_task_results WHERE task_id = :taskId ORDER BY created_at DESC")
    suspend fun getResultsByTaskId(taskId: String): List<PeriodicTaskResultEntity>
}
