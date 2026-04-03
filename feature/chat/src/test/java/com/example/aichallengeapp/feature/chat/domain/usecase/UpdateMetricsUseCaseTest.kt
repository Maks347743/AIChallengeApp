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
        val savedMetrics = mutableListOf<ChatMetrics>()
        
        override suspend fun upsertMetrics(metrics: ChatMetrics) {
            savedMetrics.add(metrics)
        }
        
        override suspend fun getMetricsByChatId(chatId: String): ChatMetrics? {
            return savedMetrics.find { it.chatId == chatId }
        }
        
        override suspend fun getAllMetrics(): List<ChatMetrics> {
            return savedMetrics.toList()
        }
        
        override suspend fun deleteMetrics(chatId: String) {
            savedMetrics.removeIf { it.chatId == chatId }
        }
    }

    val fakeRepository = FakeChatMetricsRepository()
    val useCase = UpdateMetricsUseCase(fakeRepository)

    afterTest {
        fakeRepository.savedMetrics.clear()
    }

    test("successfully updates metrics with valid inputs") {
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
            fakeRepository.savedMetrics.size shouldBe 1
            val saved = fakeRepository.savedMetrics.first()
            saved.chatId shouldBe chatId
            saved.lastRequestTokens shouldBe responseMetrics.promptTokens
            saved.lastResponseTokens shouldBe responseMetrics.completionTokens
            saved.totalTokens shouldBe 150 // 100 + 20 + 30
        }
    }

    test("handles zero tokens correctly") {
        runTest {
            // Given
            val chatId = "chat-zero"
            val currentTotalTokens = 0
            val responseMetrics = ResponseMetrics(
                promptTokens = 0,
                completionTokens = 0
            )
            
            // When
            useCase(chatId, currentTotalTokens, responseMetrics)
            
            // Then
            fakeRepository.savedMetrics.size shouldBe 1
            val saved = fakeRepository.savedMetrics.first()
            saved.totalTokens shouldBe 0
        }
    }

    test("calculates correct total when starting from zero") {
        runTest {
            // Given
            val chatId = "chat-new"
            val currentTotalTokens = 0
            val responseMetrics = ResponseMetrics(
                promptTokens = 15,
                completionTokens = 25
            )
            
            // When
            useCase(chatId, currentTotalTokens, responseMetrics)
            
            // Then
            fakeRepository.savedMetrics.size shouldBe 1
            val saved = fakeRepository.savedMetrics.first()
            saved.totalTokens shouldBe 40 // 0 + 15 + 25
        }
    }

    test("calculates correct total when adding to existing tokens") {
        runTest {
            // Given
            val chatId = "chat-existing"
            val currentTotalTokens = 500
            val responseMetrics = ResponseMetrics(
                promptTokens = 50,
                completionTokens = 75
            )
            
            // When
            useCase(chatId, currentTotalTokens, responseMetrics)
            
            // Then
            fakeRepository.savedMetrics.size shouldBe 1
            val saved = fakeRepository.savedMetrics.first()
            saved.totalTokens shouldBe 625 // 500 + 50 + 75
        }
    }

    test("throws exception when currentTotalTokens is negative") {
        runTest {
            // Given
            val chatId = "chat-negative"
            val currentTotalTokens = -1
            val responseMetrics = ResponseMetrics(
                promptTokens = 10,
                completionTokens = 20
            )
            
            // When/Then
            val exception = kotlin.runCatching {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }.exceptionOrNull()
            
            exception.shouldBeInstanceOf<IllegalArgumentException>()
            exception?.message shouldBe "currentTotalTokens must be non-negative"
            fakeRepository.savedMetrics.shouldBeEmpty()
        }
    }

    test("throws exception when promptTokens is negative") {
        runTest {
            // Given
            val chatId = "chat-negative-prompt"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(
                promptTokens = -5,
                completionTokens = 20
            )
            
            // When/Then
            val exception = kotlin.runCatching {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }.exceptionOrNull()
            
            exception.shouldBeInstanceOf<IllegalArgumentException>()
            exception?.message shouldBe "promptTokens must be non-negative"
            fakeRepository.savedMetrics.shouldBeEmpty()
        }
    }

    test("throws exception when completionTokens is negative") {
        runTest {
            // Given
            val chatId = "chat-negative-completion"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(
                promptTokens = 10,
                completionTokens = -3
            )
            
            // When/Then
            val exception = kotlin.runCatching {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }.exceptionOrNull()
            
            exception.shouldBeInstanceOf<IllegalArgumentException>()
            exception?.message shouldBe "completionTokens must be non-negative"
            fakeRepository.savedMetrics.shouldBeEmpty()
        }
    }

    test("throws exception when all inputs are negative") {
        runTest {
            // Given
            val chatId = "chat-all-negative"
            val currentTotalTokens = -10
            val responseMetrics = ResponseMetrics(
                promptTokens = -5,
                completionTokens = -3
            )
            
            // When/Then
            val exception = kotlin.runCatching {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }.exceptionOrNull()
            
            exception.shouldBeInstanceOf<IllegalArgumentException>()
            exception?.message shouldBe "currentTotalTokens must be non-negative"
            fakeRepository.savedMetrics.shouldBeEmpty()
        }
    }

    test("overwrites metrics for same chatId on subsequent calls") {
        runTest {
            // Given
            val chatId = "chat-repeated"
            
            // First call
            useCase(chatId, 0, ResponseMetrics(promptTokens = 10, completionTokens = 20))
            
            // Second call with updated total
            useCase(chatId, 30, ResponseMetrics(promptTokens = 5, completionTokens = 15))
            
            // Then
            fakeRepository.savedMetrics.size shouldBe 2 // Both calls saved
            val latest = fakeRepository.savedMetrics.last()
            latest.chatId shouldBe chatId
            latest.totalTokens shouldBe 50 // 30 + 5 + 15
        }
    }
})