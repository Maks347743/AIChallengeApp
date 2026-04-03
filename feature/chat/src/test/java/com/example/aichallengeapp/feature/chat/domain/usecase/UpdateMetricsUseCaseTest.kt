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
            val chatId = "chat-123"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(promptTokens = 20, completionTokens = 30)
            
            useCase(chatId, currentTotalTokens, responseMetrics)
            
            fakeRepository.savedMetrics.size shouldBe 1
            val saved = fakeRepository.savedMetrics.first()
            saved.chatId shouldBe chatId
            saved.lastRequestTokens shouldBe 20
            saved.lastResponseTokens shouldBe 30
            saved.totalTokens shouldBe 150 // 100 + 20 + 30
        }
    }

    test("calculates correct total when current tokens is zero") {
        runTest {
            val chatId = "chat-456"
            val currentTotalTokens = 0
            val responseMetrics = ResponseMetrics(promptTokens = 10, completionTokens = 15)
            
            useCase(chatId, currentTotalTokens, responseMetrics)
            
            val saved = fakeRepository.savedMetrics.first()
            saved.totalTokens shouldBe 25 // 0 + 10 + 15
        }
    }

    test("calculates correct total when only prompt tokens are present") {
        runTest {
            val chatId = "chat-789"
            val currentTotalTokens = 50
            val responseMetrics = ResponseMetrics(promptTokens = 5, completionTokens = 0)
            
            useCase(chatId, currentTotalTokens, responseMetrics)
            
            val saved = fakeRepository.savedMetrics.first()
            saved.totalTokens shouldBe 55 // 50 + 5 + 0
        }
    }

    test("calculates correct total when only completion tokens are present") {
        runTest {
            val chatId = "chat-abc"
            val currentTotalTokens = 75
            val responseMetrics = ResponseMetrics(promptTokens = 0, completionTokens = 25)
            
            useCase(chatId, currentTotalTokens, responseMetrics)
            
            val saved = fakeRepository.savedMetrics.first()
            saved.totalTokens shouldBe 100 // 75 + 0 + 25
        }
    }

    test("overwrites existing metrics for same chatId") {
        runTest {
            val chatId = "same-chat"
            
            // First call
            useCase(chatId, 100, ResponseMetrics(promptTokens = 10, completionTokens = 15))
            
            // Second call with same chatId
            useCase(chatId, 125, ResponseMetrics(promptTokens = 5, completionTokens = 10))
            
            fakeRepository.savedMetrics.size shouldBe 2 // Repository should have both entries
            val latest = fakeRepository.savedMetrics.last()
            latest.chatId shouldBe chatId
            latest.totalTokens shouldBe 140 // 125 + 5 + 10
        }
    }

    test("throws exception when currentTotalTokens is negative") {
        runTest {
            val chatId = "chat-neg"
            val currentTotalTokens = -1
            val responseMetrics = ResponseMetrics(promptTokens = 10, completionTokens = 15)
            
            val exception = shouldThrow<IllegalArgumentException> {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }
            
            exception.message shouldBe "currentTotalTokens must be non-negative"
            fakeRepository.savedMetrics.shouldBeEmpty()
        }
    }

    test("throws exception when promptTokens is negative") {
        runTest {
            val chatId = "chat-neg-prompt"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(promptTokens = -5, completionTokens = 15)
            
            val exception = shouldThrow<IllegalArgumentException> {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }
            
            exception.message shouldBe "promptTokens must be non-negative"
            fakeRepository.savedMetrics.shouldBeEmpty()
        }
    }

    test("throws exception when completionTokens is negative") {
        runTest {
            val chatId = "chat-neg-completion"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(promptTokens = 10, completionTokens = -3)
            
            val exception = shouldThrow<IllegalArgumentException> {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }
            
            exception.message shouldBe "completionTokens must be non-negative"
            fakeRepository.savedMetrics.shouldBeEmpty()
        }
    }

    test("handles large token counts correctly") {
        runTest {
            val chatId = "chat-large"
            val currentTotalTokens = Int.MAX_VALUE - 1000
            val responseMetrics = ResponseMetrics(promptTokens = 500, completionTokens = 500)
            
            useCase(chatId, currentTotalTokens, responseMetrics)
            
            val saved = fakeRepository.savedMetrics.first()
            saved.totalTokens shouldBe Int.MAX_VALUE // Should handle overflow correctly
        }
    }
})