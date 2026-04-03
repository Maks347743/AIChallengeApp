package com.example.aichallengeapp.feature.chat.domain.usecase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.string.shouldContain

import com.example.aichallengeapp.core.database.domain.model.ChatMetrics
import com.example.aichallengeapp.core.database.domain.model.ResponseMetrics
import com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
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
            promptTokens = 20,
            completionTokens = 30,
            totalTokens = 50,
            costUsd = 0.001
        )
        
        useCase(chatId, currentTotalTokens, responseMetrics)
        
        fakeRepository.lastUpsertedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 20,
            lastResponseTokens = 30,
            totalTokens = 150
        )
    }

    test("should calculate correct total when current tokens is zero") {
        val chatId = "chat456"
        val currentTotalTokens = 0
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 300L,
            promptTokens = 10,
            completionTokens = 15,
            totalTokens = 25,
            costUsd = 0.0005
        )
        
        useCase(chatId, currentTotalTokens, responseMetrics)
        
        fakeRepository.lastUpsertedMetrics?.totalTokens shouldBe 25
    }

    test("should throw exception when currentTotalTokens is negative") {
        val chatId = "chat789"
        val currentTotalTokens = -1
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 400L,
            promptTokens = 10,
            completionTokens = 15,
            totalTokens = 25,
            costUsd = 0.0005
        )
        
        shouldThrow<IllegalArgumentException> {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }.message shouldContain "currentTotalTokens must be non-negative"
    }

    test("should throw exception when promptTokens is negative") {
        val chatId = "chat999"
        val currentTotalTokens = 100
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 400L,
            promptTokens = -5,
            completionTokens = 15,
            totalTokens = 10,
            costUsd = 0.0005
        )
        
        shouldThrow<IllegalArgumentException> {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }.message shouldContain "promptTokens must be non-negative"
    }

    test("should throw exception when completionTokens is negative") {
        val chatId = "chat888"
        val currentTotalTokens = 100
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 400L,
            promptTokens = 10,
            completionTokens = -3,
            totalTokens = 7,
            costUsd = 0.0005
        )
        
        shouldThrow<IllegalArgumentException> {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }.message shouldContain "completionTokens must be non-negative"
    }

    test("should handle zero tokens for all values") {
        val chatId = "chatZero"
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

    test("should correctly sum large token values") {
        val chatId = "chatLarge"
        val currentTotalTokens = 1000000
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 1000L,
            promptTokens = 50000,
            completionTokens = 75000,
            totalTokens = 125000,
            costUsd = 0.1
        )
        
        useCase(chatId, currentTotalTokens, responseMetrics)
        
        fakeRepository.lastUpsertedMetrics?.totalTokens shouldBe 1125000
    }
})