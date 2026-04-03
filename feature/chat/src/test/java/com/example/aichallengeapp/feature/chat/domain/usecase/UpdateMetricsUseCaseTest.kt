package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.ChatMetrics
import com.example.aichallengeapp.core.database.domain.model.ResponseMetrics
import com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

class UpdateMetricsUseCaseTest : FunSpec({

    class FakeChatMetricsRepository : ChatMetricsRepository {
        var savedMetrics: ChatMetrics? = null
        
        override suspend fun upsertMetrics(metrics: ChatMetrics) {
            savedMetrics = metrics
        }
        
        override suspend fun getMetricsByChatId(chatId: String): ChatMetrics? {
            return savedMetrics
        }
        
        override suspend fun deleteMetricsByChatId(chatId: String) {
            savedMetrics = null
        }
    }

    val fakeRepository = FakeChatMetricsRepository()
    val useCase = UpdateMetricsUseCase(fakeRepository)

    test("should update metrics successfully with valid inputs") {
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
            val savedMetrics = fakeRepository.savedMetrics
            savedMetrics shouldBe ChatMetrics(
                chatId = chatId,
                lastRequestTokens = 20,
                lastResponseTokens = 30,
                totalTokens = 150 // 100 + 20 + 30
            )
        }
    }

    test("should throw exception when currentTotalTokens is negative") {
        runTest {
            // Given
            val chatId = "chat-123"
            val currentTotalTokens = -1
            val responseMetrics = ResponseMetrics(
                promptTokens = 20,
                completionTokens = 30
            )
            
            // When/Then
            val exception = shouldThrow<IllegalArgumentException> {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }
            
            exception.message shouldBe "currentTotalTokens must be non-negative"
        }
    }

    test("should throw exception when promptTokens is negative") {
        runTest {
            // Given
            val chatId = "chat-123"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(
                promptTokens = -5,
                completionTokens = 30
            )
            
            // When/Then
            val exception = shouldThrow<IllegalArgumentException> {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }
            
            exception.message shouldBe "promptTokens must be non-negative"
        }
    }

    test("should throw exception when completionTokens is negative") {
        runTest {
            // Given
            val chatId = "chat-123"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(
                promptTokens = 20,
                completionTokens = -10
            )
            
            // When/Then
            val exception = shouldThrow<IllegalArgumentException> {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }
            
            exception.message shouldBe "completionTokens must be non-negative"
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
            val savedMetrics = fakeRepository.savedMetrics
            savedMetrics shouldBe ChatMetrics(
                chatId = chatId,
                lastRequestTokens = 0,
                lastResponseTokens = 0,
                totalTokens = 0
            )
        }
    }

    test("should calculate correct total when adding to existing tokens") {
        runTest {
            // Given
            val chatId = "chat-789"
            val currentTotalTokens = 500
            val responseMetrics = ResponseMetrics(
                promptTokens = 150,
                completionTokens = 200
            )
            
            // When
            useCase(chatId, currentTotalTokens, responseMetrics)
            
            // Then
            val savedMetrics = fakeRepository.savedMetrics
            savedMetrics shouldBe ChatMetrics(
                chatId = chatId,
                lastRequestTokens = 150,
                lastResponseTokens = 200,
                totalTokens = 850 // 500 + 150 + 200
            )
        }
    }

    test("should overwrite previous metrics for same chatId") {
        runTest {
            // Given - first call
            val chatId = "same-chat"
            useCase(chatId, 100, ResponseMetrics(20, 30))
            
            // When - second call with same chatId
            useCase(chatId, 150, ResponseMetrics(40, 50))
            
            // Then - should have latest values
            val savedMetrics = fakeRepository.savedMetrics
            savedMetrics shouldBe ChatMetrics(
                chatId = chatId,
                lastRequestTokens = 40,
                lastResponseTokens = 50,
                totalTokens = 240 // 150 + 40 + 50
            )
        }
    }
})