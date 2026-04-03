package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.ChatMetrics
import com.example.aichallengeapp.core.database.domain.model.ResponseMetrics
import com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.flowOf

class UpdateMetricsUseCaseTest : FunSpec({

    class FakeChatMetricsRepository : ChatMetricsRepository {
        var savedMetrics: ChatMetrics? = null
        var observedChatId: String? = null
        
        override fun observeMetrics(chatId: String) = flowOf(savedMetrics)
        
        override suspend fun upsertMetrics(metrics: ChatMetrics) {
            savedMetrics = metrics
        }
        
        override suspend fun deleteMetrics(chatId: String) {
            savedMetrics = null
        }
    }

    val fakeRepository = FakeChatMetricsRepository()
    val useCase = UpdateMetricsUseCase(fakeRepository)

    test("should calculate correct total tokens and save metrics") {
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
        
        fakeRepository.savedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 20,
            lastResponseTokens = 30,
            totalTokens = 150
        )
    }

    test("should handle zero current tokens") {
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
        
        fakeRepository.savedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 10,
            lastResponseTokens = 15,
            totalTokens = 25
        )
    }

    test("should handle zero prompt and completion tokens") {
        val chatId = "chat789"
        val currentTotalTokens = 50
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 200L,
            promptTokens = 0,
            completionTokens = 0,
            totalTokens = 0,
            costUsd = 0.0
        )
        
        useCase(chatId, currentTotalTokens, responseMetrics)
        
        fakeRepository.savedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 0,
            lastResponseTokens = 0,
            totalTokens = 50
        )
    }

    test("should throw exception when currentTotalTokens is negative") {
        val chatId = "chat999"
        val currentTotalTokens = -1
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 100L,
            promptTokens = 10,
            completionTokens = 10,
            totalTokens = 20,
            costUsd = 0.0003
        )
        
        val exception = kotlin.runCatching {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }.exceptionOrNull()
        
        exception.shouldBeInstanceOf<IllegalArgumentException>()
        exception?.message shouldBe "currentTotalTokens must be non-negative"
    }

    test("should throw exception when promptTokens is negative") {
        val chatId = "chat888"
        val currentTotalTokens = 100
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 100L,
            promptTokens = -5,
            completionTokens = 10,
            totalTokens = 5,
            costUsd = 0.0002
        )
        
        val exception = kotlin.runCatching {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }.exceptionOrNull()
        
        exception.shouldBeInstanceOf<IllegalArgumentException>()
        exception?.message shouldBe "promptTokens must be non-negative"
    }

    test("should throw exception when completionTokens is negative") {
        val chatId = "chat777"
        val currentTotalTokens = 100
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 100L,
            promptTokens = 10,
            completionTokens = -3,
            totalTokens = 7,
            costUsd = 0.0001
        )
        
        val exception = kotlin.runCatching {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }.exceptionOrNull()
        
        exception.shouldBeInstanceOf<IllegalArgumentException>()
        exception?.message shouldBe "completionTokens must be non-negative"
    }

    test("should handle large token values correctly") {
        val chatId = "chat-large"
        val currentTotalTokens = 1000000
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 1000L,
            promptTokens = 50000,
            completionTokens = 30000,
            totalTokens = 80000,
            costUsd = 0.5
        )
        
        useCase(chatId, currentTotalTokens, responseMetrics)
        
        fakeRepository.savedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 50000,
            lastResponseTokens = 30000,
            totalTokens = 1080000
        )
    }
})