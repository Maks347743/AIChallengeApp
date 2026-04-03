package com.example.aichallengeapp.feature.chat.domain.usecase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.string.shouldContain

import com.example.aichallengeapp.core.database.domain.model.ChatMetrics
import com.example.aichallengeapp.core.database.domain.model.ResponseMetrics
import com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.flowOf

class UpdateMetricsUseCaseTest : FunSpec({

    class FakeChatMetricsRepository : ChatMetricsRepository {
        var lastUpsertedMetrics: ChatMetrics? = null
        var observedChatId: String? = null
        
        override fun observeMetrics(chatId: String) = flowOf<ChatMetrics?>(null)
        
        override suspend fun upsertMetrics(metrics: ChatMetrics) {
            lastUpsertedMetrics = metrics
        }
        
        override suspend fun deleteMetrics(chatId: String) {}
    }

    val fakeRepository = FakeChatMetricsRepository()
    val useCase = UpdateMetricsUseCase(fakeRepository)

    test("should upsert correct metrics when all values are valid") {
        val chatId = "chat123"
        val currentTotalTokens = 100
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 500L,
            promptTokens = 50,
            completionTokens = 30,
            totalTokens = 80,
            costUsd = 0.001
        )
        
        useCase(chatId, currentTotalTokens, responseMetrics)
        
        fakeRepository.lastUpsertedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 50,
            lastResponseTokens = 30,
            totalTokens = 180
        )
    }

    test("should handle zero current tokens correctly") {
        val chatId = "chat456"
        val currentTotalTokens = 0
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 300L,
            promptTokens = 10,
            completionTokens = 5,
            totalTokens = 15,
            costUsd = 0.0005
        )
        
        useCase(chatId, currentTotalTokens, responseMetrics)
        
        fakeRepository.lastUpsertedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 10,
            lastResponseTokens = 5,
            totalTokens = 15
        )
    }

    test("should handle zero prompt and completion tokens correctly") {
        val chatId = "chat789"
        val currentTotalTokens = 200
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 100L,
            promptTokens = 0,
            completionTokens = 0,
            totalTokens = 0,
            costUsd = 0.0
        )
        
        useCase(chatId, currentTotalTokens, responseMetrics)
        
        fakeRepository.lastUpsertedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 0,
            lastResponseTokens = 0,
            totalTokens = 200
        )
    }

    test("should throw IllegalArgumentException when currentTotalTokens is negative") {
        val chatId = "chat999"
        val currentTotalTokens = -1
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 200L,
            promptTokens = 10,
            completionTokens = 5,
            totalTokens = 15,
            costUsd = 0.0005
        )
        
        val exception = shouldThrow<IllegalArgumentException> {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }
        
        exception.message shouldContain "currentTotalTokens must be non-negative"
    }

    test("should throw IllegalArgumentException when promptTokens is negative") {
        val chatId = "chat999"
        val currentTotalTokens = 100
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 200L,
            promptTokens = -1,
            completionTokens = 5,
            totalTokens = 4,
            costUsd = 0.0005
        )
        
        val exception = shouldThrow<IllegalArgumentException> {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }
        
        exception.message shouldContain "promptTokens must be non-negative"
    }

    test("should throw IllegalArgumentException when completionTokens is negative") {
        val chatId = "chat999"
        val currentTotalTokens = 100
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 200L,
            promptTokens = 10,
            completionTokens = -1,
            totalTokens = 9,
            costUsd = 0.0005
        )
        
        val exception = shouldThrow<IllegalArgumentException> {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }
        
        exception.message shouldContain "completionTokens must be non-negative"
    }

    test("should calculate correct total with large token values") {
        val chatId = "chatLarge"
        val currentTotalTokens = 1000000
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 1000L,
            promptTokens = 50000,
            completionTokens = 25000,
            totalTokens = 75000,
            costUsd = 0.1
        )
        
        useCase(chatId, currentTotalTokens, responseMetrics)
        
        fakeRepository.lastUpsertedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 50000,
            lastResponseTokens = 25000,
            totalTokens = 1075000
        )
    }
})