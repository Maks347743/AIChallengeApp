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

    test("should update metrics correctly with valid inputs") {
        runTest {
            // Given
            val chatId = "test-chat-123"
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

    test("should handle zero tokens correctly") {
        runTest {
            // Given
            val chatId = "test-chat-zero"
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

    test("should throw exception when currentTotalTokens is negative") {
        runTest {
            // Given
            val chatId = "test-chat-negative"
            val currentTotalTokens = -1
            val responseMetrics = ResponseMetrics(
                promptTokens = 10,
                completionTokens = 10
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
            val chatId = "test-chat-negative-prompt"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(
                promptTokens = -5,
                completionTokens = 10
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
            val chatId = "test-chat-negative-completion"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(
                promptTokens = 10,
                completionTokens = -3
            )
            
            // When/Then
            val exception = shouldThrow<IllegalArgumentException> {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }
            
            exception.message shouldBe "completionTokens must be non-negative"
        }
    }

    test("should correctly calculate total with large token counts") {
        runTest {
            // Given
            val chatId = "test-chat-large"
            val currentTotalTokens = 1000000
            val responseMetrics = ResponseMetrics(
                promptTokens = 50000,
                completionTokens = 25000
            )
            
            // When
            useCase(chatId, currentTotalTokens, responseMetrics)
            
            // Then
            val savedMetrics = fakeRepository.savedMetrics
            savedMetrics shouldBe ChatMetrics(
                chatId = chatId,
                lastRequestTokens = 50000,
                lastResponseTokens = 25000,
                totalTokens = 1075000 // 1000000 + 50000 + 25000
            )
        }
    }

    test("should overwrite previous metrics for same chatId") {
        runTest {
            // Given - first call
            val chatId = "same-chat-id"
            useCase(chatId, 100, ResponseMetrics(10, 20))
            
            // When - second call with same chatId
            useCase(chatId, 130, ResponseMetrics(15, 25))
            
            // Then - should have latest values
            val savedMetrics = fakeRepository.savedMetrics
            savedMetrics shouldBe ChatMetrics(
                chatId = chatId,
                lastRequestTokens = 15,
                lastResponseTokens = 25,
                totalTokens = 170 // 130 + 15 + 25
            )
        }
    }
})