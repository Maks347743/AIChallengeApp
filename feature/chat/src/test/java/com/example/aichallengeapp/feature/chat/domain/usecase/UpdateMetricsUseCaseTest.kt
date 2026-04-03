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
        val savedMetrics = mutableListOf<ChatMetrics>()
        
        override suspend fun upsertMetrics(metrics: ChatMetrics) {
            savedMetrics.add(metrics)
        }
        
        override suspend fun getMetricsByChatId(chatId: String): ChatMetrics? {
            return savedMetrics.find { it.chatId == chatId }
        }
        
        override suspend fun deleteMetricsByChatId(chatId: String) {
            savedMetrics.removeIf { it.chatId == chatId }
        }
    }

    val fakeRepository = FakeChatMetricsRepository()
    val useCase = UpdateMetricsUseCase(fakeRepository)

    afterEach {
        fakeRepository.savedMetrics.clear()
    }

    test("should save correct metrics with valid positive values") {
        runTest {
            val chatId = "chat-123"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(promptTokens = 50, completionTokens = 30)
            
            useCase(chatId, currentTotalTokens, responseMetrics)
            
            fakeRepository.savedMetrics.size shouldBe 1
            val saved = fakeRepository.savedMetrics.first()
            saved.chatId shouldBe chatId
            saved.lastRequestTokens shouldBe 50
            saved.lastResponseTokens shouldBe 30
            saved.totalTokens shouldBe 180 // 100 + 50 + 30
        }
    }

    test("should save correct metrics with zero values") {
        runTest {
            val chatId = "chat-456"
            val currentTotalTokens = 0
            val responseMetrics = ResponseMetrics(promptTokens = 0, completionTokens = 0)
            
            useCase(chatId, currentTotalTokens, responseMetrics)
            
            fakeRepository.savedMetrics.size shouldBe 1
            val saved = fakeRepository.savedMetrics.first()
            saved.chatId shouldBe chatId
            saved.lastRequestTokens shouldBe 0
            saved.lastResponseTokens shouldBe 0
            saved.totalTokens shouldBe 0
        }
    }

    test("should handle large token counts correctly") {
        runTest {
            val chatId = "chat-789"
            val currentTotalTokens = 1000000
            val responseMetrics = ResponseMetrics(promptTokens = 50000, completionTokens = 30000)
            
            useCase(chatId, currentTotalTokens, responseMetrics)
            
            fakeRepository.savedMetrics.size shouldBe 1
            val saved = fakeRepository.savedMetrics.first()
            saved.totalTokens shouldBe 1080000 // 1000000 + 50000 + 30000
        }
    }

    test("should throw exception when currentTotalTokens is negative") {
        runTest {
            val chatId = "chat-999"
            val currentTotalTokens = -1
            val responseMetrics = ResponseMetrics(promptTokens = 10, completionTokens = 5)
            
            val exception = shouldThrow<IllegalArgumentException> {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }
            
            exception.message shouldBe "currentTotalTokens must be non-negative"
            fakeRepository.savedMetrics.shouldBeEmpty()
        }
    }

    test("should throw exception when promptTokens is negative") {
        runTest {
            val chatId = "chat-999"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(promptTokens = -5, completionTokens = 10)
            
            val exception = shouldThrow<IllegalArgumentException> {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }
            
            exception.message shouldBe "promptTokens must be non-negative"
            fakeRepository.savedMetrics.shouldBeEmpty()
        }
    }

    test("should throw exception when completionTokens is negative") {
        runTest {
            val chatId = "chat-999"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(promptTokens = 10, completionTokens = -3)
            
            val exception = shouldThrow<IllegalArgumentException> {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }
            
            exception.message shouldBe "completionTokens must be non-negative"
            fakeRepository.savedMetrics.shouldBeEmpty()
        }
    }

    test("should throw exception when all values are negative") {
        runTest {
            val chatId = "chat-999"
            val currentTotalTokens = -10
            val responseMetrics = ResponseMetrics(promptTokens = -5, completionTokens = -3)
            
            val exception = shouldThrow<IllegalArgumentException> {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }
            
            // Should fail on first require check (currentTotalTokens)
            exception.message shouldBe "currentTotalTokens must be non-negative"
            fakeRepository.savedMetrics.shouldBeEmpty()
        }
    }

    test("should update metrics for same chatId multiple times") {
        runTest {
            val chatId = "same-chat"
            
            // First call
            useCase(chatId, 0, ResponseMetrics(promptTokens = 10, completionTokens = 5))
            
            // Second call with updated total
            useCase(chatId, 15, ResponseMetrics(promptTokens = 20, completionTokens = 10))
            
            fakeRepository.savedMetrics.size shouldBe 2
            val firstSaved = fakeRepository.savedMetrics[0]
            val secondSaved = fakeRepository.savedMetrics[1]
            
            firstSaved.totalTokens shouldBe 15 // 0 + 10 + 5
            secondSaved.totalTokens shouldBe 45 // 15 + 20 + 10
            secondSaved.chatId shouldBe chatId
        }
    }
})