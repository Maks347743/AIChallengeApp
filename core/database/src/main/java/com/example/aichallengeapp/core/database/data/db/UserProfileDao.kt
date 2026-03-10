package com.example.aichallengeapp.core.database.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles ORDER BY createdAt ASC")
    fun getAllProfiles(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE id = :id")
    suspend fun getById(id: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Delete
    suspend fun delete(profile: UserProfileEntity)

    @Query("DELETE FROM user_profiles WHERE id = :id")
    suspend fun deleteById(id: String)
}
