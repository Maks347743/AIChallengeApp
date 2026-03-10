package com.example.aichallengeapp.feature.explore.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichallengeapp.feature.explore.domain.GitHubMcpRepository
import com.example.aichallengeapp.feature.explore.domain.GitHubMcpResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class ExploreGitHubViewModel(
    private val mcpRepository: GitHubMcpRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExploreGitHubUiState>(ExploreGitHubUiState.Loading)
    val uiState: StateFlow<ExploreGitHubUiState> = _uiState.asStateFlow()

    init {
        loadTools()
    }

    fun retry() {
        _uiState.value = ExploreGitHubUiState.Loading
        loadTools()
    }

    private fun loadTools() {
        viewModelScope.launch {
            when (val result = mcpRepository.fetchTools()) {
                is GitHubMcpResult.Success -> {
                    Timber.tag(TAG).d("Loaded ${result.tools.size} MCP tools")
                    if (result.tools.isEmpty()) {
                        _uiState.value = ExploreGitHubUiState.Empty
                    } else {
                        _uiState.value = ExploreGitHubUiState.Success(result.tools)
                    }
                }
                is GitHubMcpResult.AuthError -> {
                    Timber.tag(TAG).e("Auth error: ${result.message}")
                    _uiState.value = ExploreGitHubUiState.Error(
                        message = result.message,
                        isAuthError = true
                    )
                }
                is GitHubMcpResult.Error -> {
                    Timber.tag(TAG).e("Error: ${result.message}")
                    _uiState.value = ExploreGitHubUiState.Error(message = result.message)
                }
            }
        }
    }

    private companion object {
        const val TAG = "ExploreGitHubViewModel"
    }
}
