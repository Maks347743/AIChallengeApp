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

    context("with valid inputs") {
        test("should calculate correct total and save metrics") {
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

        test("should handle zero tokens correctly") {
            runTest {
                val chatId = "chat-456"
                val currentTotalTokens = 0
                val responseMetrics = ResponseMetrics(promptTokens = 0, completionTokens = 0)
                
                useCase(chatId, currentTotalTokens, responseMetrics)
                
                val saved = fakeRepository.savedMetrics.first()
                saved.totalTokens shouldBe 0
                saved.lastRequestTokens shouldBe 0
                saved.lastResponseTokens shouldBe 0
            }
        }

        test("should update existing chat metrics") {
            runTest {
                val chatId = "chat-789"
                
                // First call
                useCase(chatId, 100, ResponseMetrics(promptTokens = 20, completionTokens = 10))
                
                // Second call with updated current total
                useCase(chatId, 130, ResponseMetrics(promptTokens = 15, completionTokens = 5))
                
                fakeRepository.savedMetrics.size shouldBe 2
                val latest = fakeRepository.savedMetrics.last()
                latest.totalTokens shouldBe 150 // 130 + 15 + 5
                latest.lastRequestTokens shouldBe 15
                latest.lastResponseTokens shouldBe 5
            }
        }
    }

    context("with invalid inputs") {
        test("should throw exception when currentTotalTokens is negative") {
            runTest {
                val exception = shouldThrow<IllegalArgumentException> {
                    useCase("chat-1", -1, ResponseMetrics(promptTokens = 10, completionTokens = 5))
                }
                exception.message shouldBe "currentTotalTokens must be non-negative"
                fakeRepository.savedMetrics.shouldBeEmpty()
            }
        }

        test("should throw exception when promptTokens is negative") {
            runTest {
                val exception = shouldThrow<IllegalArgumentException> {
                    useCase("chat-2", 100, ResponseMetrics(promptTokens = -5, completionTokens = 10))
                }
                exception.message shouldBe "promptTokens must be non-negative"
                fakeRepository.savedMetrics.shouldBeEmpty()
            }
        }

        test("should throw exception when completionTokens is negative") {
            runTest {
                val exception = shouldThrow<IllegalArgumentException> {
                    useCase("chat-3", 100, ResponseMetrics(promptTokens = 10, completionTokens = -3))
                }
                exception.message shouldBe "completionTokens must be non-negative"
                fakeRepository.savedMetrics.shouldBeEmpty()
            }
        }

        test("should throw exception when all values are negative") {
            runTest {
                val exception = shouldThrow<IllegalArgumentException> {
                    useCase("chat-4", -10, ResponseMetrics(promptTokens = -5, completionTokens = -3))
                }
                exception.message shouldBe "currentTotalTokens must be non-negative"
                fakeRepository.savedMetrics.shouldBeEmpty()
            }
        }
    }

    context("with large token values") {
        test("should handle large token counts correctly") {
            runTest {
                val chatId = "chat-large"
                val currentTotalTokens = Int.MAX_VALUE - 1000
                val responseMetrics = ResponseMetrics(promptTokens = 500, completionTokens = 500)
                
                useCase(chatId, currentTotalTokens, responseMetrics)
                
                val saved = fakeRepository.savedMetrics.first()
                saved.totalTokens shouldBe Int.MAX_VALUE
                saved.lastRequestTokens shouldBe 500
                saved.lastResponseTokens shouldBe 500
            }
        }
    }
})