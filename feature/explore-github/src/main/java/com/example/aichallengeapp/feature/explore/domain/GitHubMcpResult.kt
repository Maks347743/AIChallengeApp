package com.example.aichallengeapp.feature.explore.domain

import com.example.aichallengeapp.core.mcp.model.McpTool

sealed interface GitHubMcpResult {
    data class Success(val tools: List<McpTool>) : GitHubMcpResult
    data class AuthError(val message: String) : GitHubMcpResult
    data class Error(val message: String) : GitHubMcpResult
}
