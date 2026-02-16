package com.example.aichallengeapp.domain.usecase

import com.example.aichallengeapp.domain.repository.ChatRepository

class CheckConnectionUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(): Result<String> {
        return chatRepository.sendMessage(TEST_MESSAGE)
    }

    companion object {
        private const val TEST_MESSAGE = "Hello"
    }
}
