package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.ChatMetrics
import com.example.aichallengeapp.core.database.domain.model.ResponseMetrics
import com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest

class UpdateMetricsUseCaseTest : FunSpec({

    class FakeChatMetricsRepository : ChatMetricsRepository {
        val upsertedMetrics = mutableListOf<ChatMetrics>()
        
        override suspend fun upsertMetrics(metrics: ChatMetrics) {
            upsertedMetrics.add(metrics)
        }
        
        override suspend fun getMetricsByChatId(chatId: String): ChatMetrics? {
            return upsertedMetrics.lastOrNull { it.chatId == chatId }
        }
        
        override suspend fun getAllMetrics(): List<ChatMetrics> {
            return upsertedMetrics.toList()
        }
        
        fun clear() {
            upsertedMetrics.clear()
        }
    }

    val fakeRepository = FakeChatMetricsRepository()
    val useCase = UpdateMetricsUseCase(fakeRepository)

    beforeEach {
        fakeRepository.clear()
    }

    test("should update metrics correctly with valid inputs") {
        runTest {
            // Given
            val chatId = "chat-123"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(
                promptTokens = 20,
                completionTokens = 30
            )

            // When
            useCase(chatId, currentTotalTokens, responseMetrics)

            // Then
            fakeRepository.upsertedMetrics.size shouldBe 1
            val savedMetrics = fakeRepository.upsertedMetrics[0]
            savedMetrics.chatId shouldBe chatId
            savedMetrics.lastRequestTokens shouldBe responseMetrics.promptTokens
            savedMetrics.lastResponseTokens shouldBe responseMetrics.completionTokens
            savedMetrics.totalTokens shouldBe 150 // 100 + 20 + 30
        }
    }

    test("should handle zero tokens correctly") {
        runTest {
            // Given
            val chatId = "chat-456"
            val currentTotalTokens = 0
            val responseMetrics = ResponseMetrics(
                promptTokens = 0,
                completionTokens = 0
            )

            // When
            useCase(chatId, currentTotalTokens, responseMetrics)

            // Then
            fakeRepository.upsertedMetrics.size shouldBe 1
            val savedMetrics = fakeRepository.upsertedMetrics[0]
            savedMetrics.totalTokens shouldBe 0
        }
    }

    test("should calculate new total correctly with large numbers") {
        runTest {
            // Given
            val chatId = "chat-789"
            val currentTotalTokens = 1000
            val responseMetrics = ResponseMetrics(
                promptTokens = 500,
                completionTokens = 750
            )

            // When
            useCase(chatId, currentTotalTokens, responseMetrics)

            // Then
            fakeRepository.upsertedMetrics.size shouldBe 1
            val savedMetrics = fakeRepository.upsertedMetrics[0]
            savedMetrics.totalTokens shouldBe 2250 // 1000 + 500 + 750
        }
    }

    test("should throw IllegalArgumentException when currentTotalTokens is negative") {
        runTest {
            // Given
            val chatId = "chat-999"
            val currentTotalTokens = -1
            val responseMetrics = ResponseMetrics(
                promptTokens = 10,
                completionTokens = 20
            )

            // When & Then
            val exception = kotlin.runCatching {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }.exceptionOrNull()
            
            exception.shouldBeInstanceOf<IllegalArgumentException>()
            exception?.message shouldBe "currentTotalTokens must be non-negative"
            fakeRepository.upsertedMetrics.shouldBeEmpty()
        }
    }

    test("should throw IllegalArgumentException when promptTokens is negative") {
        runTest {
            // Given
            val chatId = "chat-888"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(
                promptTokens = -5,
                completionTokens = 20
            )

            // When & Then
            val exception = kotlin.runCatching {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }.exceptionOrNull()
            
            exception.shouldBeInstanceOf<IllegalArgumentException>()
            exception?.message shouldBe "promptTokens must be non-negative"
            fakeRepository.upsertedMetrics.shouldBeEmpty()
        }
    }

    test("should throw IllegalArgumentException when completionTokens is negative") {
        runTest {
            // Given
            val chatId = "chat-777"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(
                promptTokens = 10,
                completionTokens = -3
            )

            // When & Then
            val exception = kotlin.runCatching {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }.exceptionOrNull()
            
            exception.shouldBeInstanceOf<IllegalArgumentException>()
            exception?.message shouldBe "completionTokens must be non-negative"
            fakeRepository.upsertedMetrics.shouldBeEmpty()
        }
    }

    test("should handle multiple calls correctly") {
        runTest {
            // Given
            val chatId = "chat-multi"
            
            // First call
            useCase(chatId, 0, ResponseMetrics(promptTokens = 10, completionTokens = 20))
            
            // Second call - simulate getting current total from repository
            val firstMetrics = fakeRepository.upsertedMetrics[0]
            useCase(chatId, firstMetrics.totalTokens, ResponseMetrics(promptTokens = 15, completionTokens = 25))

            // Then
            fakeRepository.upsertedMetrics.size shouldBe 2
            val secondMetrics = fakeRepository.upsertedMetrics[1]
            secondMetrics.totalTokens shouldBe 70 // 0 + 10 + 20 + 15 + 25
        }
    }
})