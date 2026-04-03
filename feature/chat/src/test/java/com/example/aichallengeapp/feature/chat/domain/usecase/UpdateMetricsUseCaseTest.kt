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
        
        override suspend fun getMetrics(chatId: String): ChatMetrics? {
            return savedMetrics?.takeIf { it.chatId == chatId }
        }
        
        override suspend fun deleteMetrics(chatId: String) {
            if (savedMetrics?.chatId == chatId) {
                savedMetrics = null
            }
        }
    }

    val fakeRepository = FakeChatMetricsRepository()
    val useCase = UpdateMetricsUseCase(fakeRepository)

    test("should update metrics correctly with positive values") {
        runTest {
            // Given
            val chatId = "chat-123"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(
                promptTokens = 50,
                completionTokens = 30
            )
            
            // When
            useCase(chatId, currentTotalTokens, responseMetrics)
            
            // Then
            val savedMetrics = fakeRepository.savedMetrics
            savedMetrics shouldBe ChatMetrics(
                chatId = chatId,
                lastRequestTokens = 50,
                lastResponseTokens = 30,
                totalTokens = 180 // 100 + 50 + 30
            )
        }
    }

    test("should update metrics correctly with zero values") {
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

    test("should handle large token counts correctly") {
        runTest {
            // Given
            val chatId = "chat-789"
            val currentTotalTokens = Int.MAX_VALUE - 1000
            val responseMetrics = ResponseMetrics(
                promptTokens = 500,
                completionTokens = 500
            )
            
            // When
            useCase(chatId, currentTotalTokens, responseMetrics)
            
            // Then
            val savedMetrics = fakeRepository.savedMetrics
            savedMetrics shouldBe ChatMetrics(
                chatId = chatId,
                lastRequestTokens = 500,
                lastResponseTokens = 500,
                totalTokens = Int.MAX_VALUE // (MAX_VALUE - 1000) + 500 + 500
            )
        }
    }

    test("should throw exception when currentTotalTokens is negative") {
        runTest {
            // Given
            val chatId = "chat-999"
            val currentTotalTokens = -1
            val responseMetrics = ResponseMetrics(
                promptTokens = 10,
                completionTokens = 20
            )
            
            // When / Then
            val exception = shouldThrow<IllegalArgumentException> {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }
            
            exception.message shouldBe "currentTotalTokens must be non-negative"
        }
    }

    test("should throw exception when promptTokens is negative") {
        runTest {
            // Given
            val chatId = "chat-888"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(
                promptTokens = -5,
                completionTokens = 20
            )
            
            // When / Then
            val exception = shouldThrow<IllegalArgumentException> {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }
            
            exception.message shouldBe "promptTokens must be non-negative"
        }
    }

    test("should throw exception when completionTokens is negative") {
        runTest {
            // Given
            val chatId = "chat-777"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(
                promptTokens = 10,
                completionTokens = -3
            )
            
            // When / Then
            val exception = shouldThrow<IllegalArgumentException> {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }
            
            exception.message shouldBe "completionTokens must be non-negative"
        }
    }

    test("should throw exception when all inputs are negative") {
        runTest {
            // Given
            val chatId = "chat-666"
            val currentTotalTokens = -10
            val responseMetrics = ResponseMetrics(
                promptTokens = -5,
                completionTokens = -3
            )
            
            // When / Then
            val exception = shouldThrow<IllegalArgumentException> {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }
            
            // Should fail on first require check
            exception.message shouldBe "currentTotalTokens must be non-negative"
        }
    }

    test("should overwrite previous metrics for same chatId") {
        runTest {
            // Given - first call
            val chatId = "same-chat"
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