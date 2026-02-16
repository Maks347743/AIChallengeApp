package com.example.aichallengeapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichallengeapp.domain.usecase.CheckConnectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val checkConnectionUseCase: CheckConnectionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.CheckConnection -> checkConnection()
        }
    }

    private fun checkConnection() {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update {
                it.copy(status = ConnectionStatus.Loading, response = null, error = null)
            }

            checkConnectionUseCase()
                .onSuccess { responseText ->
                    _state.update {
                        it.copy(status = ConnectionStatus.Success, response = responseText)
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            status = ConnectionStatus.Error,
                            error = throwable.message ?: "Unknown error"
                        )
                    }
                }
        }
    }
}
