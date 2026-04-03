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
        var lastUpsertedMetrics: ChatMetrics? = null
        
        override suspend fun upsertMetrics(metrics: ChatMetrics) {
            lastUpsertedMetrics = metrics
        }
        
        override suspend fun getMetrics(chatId: String): ChatMetrics? {
            return lastUpsertedMetrics
        }
        
        override suspend fun deleteMetrics(chatId: String) {
            lastUpsertedMetrics = null
        }
    }

    val fakeRepository = FakeChatMetricsRepository()
    val useCase = UpdateMetricsUseCase(fakeRepository)

    test("should update metrics correctly with positive values") {
        runTest {
            val chatId = "chat-123"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(
                promptTokens = 50,
                completionTokens = 30
            )
            
            useCase(chatId, currentTotalTokens, responseMetrics)
            
            val expectedMetrics = ChatMetrics(
                chatId = chatId,
                lastRequestTokens = 50,
                lastResponseTokens = 30,
                totalTokens = 180 // 100 + 50 + 30
            )
            
            fakeRepository.lastUpsertedMetrics shouldBe expectedMetrics
        }
    }

    test("should update metrics correctly with zero values") {
        runTest {
            val chatId = "chat-456"
            val currentTotalTokens = 0
            val responseMetrics = ResponseMetrics(
                promptTokens = 0,
                completionTokens = 0
            )
            
            useCase(chatId, currentTotalTokens, responseMetrics)
            
            val expectedMetrics = ChatMetrics(
                chatId = chatId,
                lastRequestTokens = 0,
                lastResponseTokens = 0,
                totalTokens = 0
            )
            
            fakeRepository.lastUpsertedMetrics shouldBe expectedMetrics
        }
    }

    test("should handle large token counts correctly") {
        runTest {
            val chatId = "chat-789"
            val currentTotalTokens = 1000
            val responseMetrics = ResponseMetrics(
                promptTokens = 500,
                completionTokens = 300
            )
            
            useCase(chatId, currentTotalTokens, responseMetrics)
            
            val expectedMetrics = ChatMetrics(
                chatId = chatId,
                lastRequestTokens = 500,
                lastResponseTokens = 300,
                totalTokens = 1800 // 1000 + 500 + 300
            )
            
            fakeRepository.lastUpsertedMetrics shouldBe expectedMetrics
        }
    }

    test("should throw exception when currentTotalTokens is negative") {
        runTest {
            val chatId = "chat-999"
            val currentTotalTokens = -1
            val responseMetrics = ResponseMetrics(
                promptTokens = 10,
                completionTokens = 5
            )
            
            val exception = shouldThrow<IllegalArgumentException> {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }
            
            exception.message shouldBe "currentTotalTokens must be non-negative"
        }
    }

    test("should throw exception when promptTokens is negative") {
        runTest {
            val chatId = "chat-888"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(
                promptTokens = -5,
                completionTokens = 10
            )
            
            val exception = shouldThrow<IllegalArgumentException> {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }
            
            exception.message shouldBe "promptTokens must be non-negative"
        }
    }

    test("should throw exception when completionTokens is negative") {
        runTest {
            val chatId = "chat-777"
            val currentTotalTokens = 100
            val responseMetrics = ResponseMetrics(
                promptTokens = 10,
                completionTokens = -3
            )
            
            val exception = shouldThrow<IllegalArgumentException> {
                useCase(chatId, currentTotalTokens, responseMetrics)
            }
            
            exception.message shouldBe "completionTokens must be non-negative"
        }
    }

    test("should correctly calculate total when only prompt tokens are present") {
        runTest {
            val chatId = "chat-555"
            val currentTotalTokens = 200
            val responseMetrics = ResponseMetrics(
                promptTokens = 100,
                completionTokens = 0
            )
            
            useCase(chatId, currentTotalTokens, responseMetrics)
            
            val expectedMetrics = ChatMetrics(
                chatId = chatId,
                lastRequestTokens = 100,
                lastResponseTokens = 0,
                totalTokens = 300 // 200 + 100 + 0
            )
            
            fakeRepository.lastUpsertedMetrics shouldBe expectedMetrics
        }
    }

    test("should correctly calculate total when only completion tokens are present") {
        runTest {
            val chatId = "chat-444"
            val currentTotalTokens = 150
            val responseMetrics = ResponseMetrics(
                promptTokens = 0,
                completionTokens = 75
            )
            
            useCase(chatId, currentTotalTokens, responseMetrics)
            
            val expectedMetrics = ChatMetrics(
                chatId = chatId,
                lastRequestTokens = 0,
                lastResponseTokens = 75,
                totalTokens = 225 // 150 + 0 + 75
            )
            
            fakeRepository.lastUpsertedMetrics shouldBe expectedMetrics
        }
    }
})