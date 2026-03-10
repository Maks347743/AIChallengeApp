package com.example.aichallengeapp.feature.explore.presentation

import com.example.aichallengeapp.core.mcp.model.McpTool

sealed interface ExploreGitHubUiState {
    data object Loading : ExploreGitHubUiState
    data class Success(val tools: List<McpTool>) : ExploreGitHubUiState
    data object Empty : ExploreGitHubUiState
    data class Error(val message: String, val isAuthError: Boolean = false) : ExploreGitHubUiState
}
