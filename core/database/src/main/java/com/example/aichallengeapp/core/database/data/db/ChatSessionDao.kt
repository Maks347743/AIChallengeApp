package com.example.aichallengeapp.core.database.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatSessionDao {

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getById(id: String): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ChatSessionEntity)

    @Delete
    suspend fun delete(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE chat_sessions SET settings_json = :settingsJson WHERE id = :id")
    suspend fun updateSettingsJson(id: String, settingsJson: String): Int

    @Query("SELECT * FROM chat_sessions WHERE checkpoint_group_id = :groupId ORDER BY branch_index ASC")
    suspend fun getSessionsByGroup(groupId: String): List<ChatSessionEntity>

    @Query("UPDATE chat_sessions SET checkpoint_group_id = :groupId, branch_index = :branchIndex WHERE id = :id")
    suspend fun updateCheckpointFields(id: String, groupId: String, branchIndex: Int): Int
}
