package com.example.aichallengeapp.feature.chat.domain.usecase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.string.shouldContain

import com.example.aichallengeapp.core.database.domain.model.ChatMetrics
import com.example.aichallengeapp.core.database.domain.model.ResponseMetrics
import com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.flowOf

class UpdateMetricsUseCaseTest : FunSpec({

    inner class FakeChatMetricsRepository : ChatMetricsRepository {
        var lastUpsertedMetrics: ChatMetrics? = null
        var observedChatId: String? = null
        
        override fun observeMetrics(chatId: String) = flowOf<ChatMetrics?>(null)
        
        override suspend fun upsertMetrics(metrics: ChatMetrics) {
            lastUpsertedMetrics = metrics
        }
        
        override suspend fun deleteMetrics(chatId: String) {}
    }

    test("successfully updates metrics with valid inputs") {
        val fakeRepo = FakeChatMetricsRepository()
        val useCase = UpdateMetricsUseCase(fakeRepo)
        
        val chatId = "chat-123"
        val currentTotalTokens = 100
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 500L,
            promptTokens = 50,
            completionTokens = 30,
            totalTokens = 80,
            costUsd = 0.001
        )
        
        useCase(chatId, currentTotalTokens, responseMetrics)
        
        fakeRepo.lastUpsertedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 50,
            lastResponseTokens = 30,
            totalTokens = 180 // 100 + 50 + 30
        )
    }

    test("handles zero tokens correctly") {
        val fakeRepo = FakeChatMetricsRepository()
        val useCase = UpdateMetricsUseCase(fakeRepo)
        
        val chatId = "chat-zero"
        val currentTotalTokens = 0
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 100L,
            promptTokens = 0,
            completionTokens = 0,
            totalTokens = 0,
            costUsd = 0.0
        )
        
        useCase(chatId, currentTotalTokens, responseMetrics)
        
        fakeRepo.lastUpsertedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 0,
            lastResponseTokens = 0,
            totalTokens = 0
        )
    }

    test("throws exception when currentTotalTokens is negative") {
        val fakeRepo = FakeChatMetricsRepository()
        val useCase = UpdateMetricsUseCase(fakeRepo)
        
        val chatId = "chat-123"
        val currentTotalTokens = -1
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 500L,
            promptTokens = 50,
            completionTokens = 30,
            totalTokens = 80,
            costUsd = 0.001
        )
        
        val exception = shouldThrow<IllegalArgumentException> {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }
        
        exception.message shouldContain "currentTotalTokens must be non-negative"
        fakeRepo.lastUpsertedMetrics.shouldBeNull()
    }

    test("throws exception when promptTokens is negative") {
        val fakeRepo = FakeChatMetricsRepository()
        val useCase = UpdateMetricsUseCase(fakeRepo)
        
        val chatId = "chat-123"
        val currentTotalTokens = 100
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 500L,
            promptTokens = -1,
            completionTokens = 30,
            totalTokens = 29,
            costUsd = 0.001
        )
        
        val exception = shouldThrow<IllegalArgumentException> {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }
        
        exception.message shouldContain "promptTokens must be non-negative"
        fakeRepo.lastUpsertedMetrics.shouldBeNull()
    }

    test("throws exception when completionTokens is negative") {
        val fakeRepo = FakeChatMetricsRepository()
        val useCase = UpdateMetricsUseCase(fakeRepo)
        
        val chatId = "chat-123"
        val currentTotalTokens = 100
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 500L,
            promptTokens = 50,
            completionTokens = -1,
            totalTokens = 49,
            costUsd = 0.001
        )
        
        val exception = shouldThrow<IllegalArgumentException> {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }
        
        exception.message shouldContain "completionTokens must be non-negative"
        fakeRepo.lastUpsertedMetrics.shouldBeNull()
    }

    test("correctly calculates total with large token counts") {
        val fakeRepo = FakeChatMetricsRepository()
        val useCase = UpdateMetricsUseCase(fakeRepo)
        
        val chatId = "chat-large"
        val currentTotalTokens = 10000
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 2000L,
            promptTokens = 5000,
            completionTokens = 3000,
            totalTokens = 8000,
            costUsd = 0.1
        )
        
        useCase(chatId, currentTotalTokens, responseMetrics)
        
        fakeRepo.lastUpsertedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 5000,
            lastResponseTokens = 3000,
            totalTokens = 18000 // 10000 + 5000 + 3000
        )
    }

    test("ignores other ResponseMetrics fields (responseTimeMs, totalTokens, costUsd) in calculation") {
        val fakeRepo = FakeChatMetricsRepository()
        val useCase = UpdateMetricsUseCase(fakeRepo)
        
        val chatId = "chat-ignore"
        val currentTotalTokens = 100
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 999L, // Should be ignored
            promptTokens = 20,
            completionTokens = 10,
            totalTokens = 999, // Should be ignored - we use prompt+completion
            costUsd = 999.0 // Should be ignored
        )
        
        useCase(chatId, currentTotalTokens, responseMetrics)
        
        fakeRepo.lastUpsertedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 20,
            lastResponseTokens = 10,
            totalTokens = 130 // 100 + 20 + 10 (NOT 100 + 999)
        )
    }
})