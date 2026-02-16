package com.example.aichallengeapp.presentation

sealed interface HomeIntent {
    data object CheckConnection : HomeIntent
}

enum class ConnectionStatus {
    Idle,
    Loading,
    Success,
    Error
}

data class HomeState(
    val status: ConnectionStatus = ConnectionStatus.Idle,
    val response: String? = null,
    val error: String? = null
) {
    val isLoading: Boolean get() = status == ConnectionStatus.Loading
}
