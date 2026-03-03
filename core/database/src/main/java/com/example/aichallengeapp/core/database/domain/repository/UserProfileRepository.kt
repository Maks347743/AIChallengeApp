package com.example.aichallengeapp.core.database.domain.repository

import com.example.aichallengeapp.core.database.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun getAllProfiles(): Flow<List<UserProfile>>
    suspend fun getById(id: String): UserProfile?
    suspend fun upsert(profile: UserProfile)
    suspend fun delete(profileId: String)
}
