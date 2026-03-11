package com.example.aichallengeapp.core.database.data.repository

import com.example.aichallengeapp.core.database.data.db.UserProfileDao
import com.example.aichallengeapp.core.database.data.db.UserProfileEntity
import com.example.aichallengeapp.core.database.domain.model.Constraint
import com.example.aichallengeapp.core.database.domain.model.UserProfile
import com.example.aichallengeapp.core.database.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class UserProfileRepositoryImpl(
    private val dao: UserProfileDao,
    private val json: Json
) : UserProfileRepository {

    override fun getAllProfiles(): Flow<List<UserProfile>> =
        dao.getAllProfiles().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: String): UserProfile? =
        dao.getById(id)?.toDomain()

    override suspend fun upsert(profile: UserProfile) =
        dao.upsert(profile.toEntity())

    override suspend fun delete(profileId: String) {
        dao.deleteById(profileId)
    }

    private fun UserProfileEntity.toDomain() = UserProfile(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
        constraints = constraintsJson?.let {
            runCatching { json.decodeFromString<List<Constraint>>(it) }.getOrDefault(emptyList())
        } ?: emptyList()
    )

    private fun UserProfile.toEntity() = UserProfileEntity(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
        constraintsJson = if (constraints.isEmpty()) null else json.encodeToString(constraints)
    )
}
