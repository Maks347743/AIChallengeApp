package com.example.aichallengeapp.feature.chat.domain.usecase
import io.kotest.matchers.collections.shouldContain

import com.example.aichallengeapp.core.database.domain.model.ChatMetrics
import com.example.aichallengeapp.core.database.domain.model.ResponseMetrics
import com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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

    test("should handle zero tokens correctly") {
        val chatId = "chat456"
        val currentTotalTokens = 0
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 200L,
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
            totalTokens = 0
        )
    }

    test("should throw exception when currentTotalTokens is negative") {
        val chatId = "chat789"
        val currentTotalTokens = -1
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 300L,
            promptTokens = 10,
            completionTokens = 5,
            totalTokens = 15,
            costUsd = 0.0005
        )
        
        val exception = kotlin.runCatching {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }.exceptionOrNull()
        
        exception shouldBe IllegalArgumentException::class
        exception?.message shouldContain "currentTotalTokens must be non-negative"
    }

    test("should throw exception when promptTokens is negative") {
        val chatId = "chat999"
        val currentTotalTokens = 100
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 400L,
            promptTokens = -5,
            completionTokens = 10,
            totalTokens = 5,
            costUsd = 0.0003
        )
        
        val exception = kotlin.runCatching {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }.exceptionOrNull()
        
        exception shouldBe IllegalArgumentException::class
        exception?.message shouldContain "promptTokens must be non-negative"
    }

    test("should throw exception when completionTokens is negative") {
        val chatId = "chat888"
        val currentTotalTokens = 100
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 400L,
            promptTokens = 10,
            completionTokens = -3,
            totalTokens = 7,
            costUsd = 0.0002
        )
        
        val exception = kotlin.runCatching {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }.exceptionOrNull()
        
        exception shouldBe IllegalArgumentException::class
        exception?.message shouldContain "completionTokens must be non-negative"
    }

    test("should correctly calculate total with large numbers") {
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