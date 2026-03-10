package com.example.aichallengeapp.feature.explore.domain

interface GitHubMcpRepository {
    suspend fun fetchTools(): GitHubMcpResult
}
