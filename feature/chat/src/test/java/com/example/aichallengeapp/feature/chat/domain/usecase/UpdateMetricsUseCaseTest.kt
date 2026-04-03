package com.example.aichallengeapp.feature.chat.domain.usecase

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import com.example.aichallengeapp.core.database.domain.model.ChatMetrics
import com.example.aichallengeapp.core.database.domain.model.ResponseMetrics
import com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository
import kotlinx.coroutines.flow.Flow

class UpdateMetricsUseCaseTest : FunSpec({
    
    class FakeChatMetricsRepository : ChatMetricsRepository {
        private val metricsMap = mutableMapOf<String, ChatMetrics>()
        
        override fun observeMetrics(chatId: String): Flow<ChatMetrics?> {
            throw NotImplementedError("Not needed for this test")
        }
        
        override suspend fun upsertMetrics(metrics: ChatMetrics) {
            metricsMap[metrics.chatId] = metrics
        }
        
        override suspend fun deleteMetrics(chatId: String) {
            metricsMap.remove(chatId)
        }
        
        fun getMetrics(chatId: String): ChatMetrics? = metricsMap[chatId]
        fun clear() = metricsMap.clear()
    }
    
    val fakeRepository = FakeChatMetricsRepository()
    val useCase = UpdateMetricsUseCase(fakeRepository)
    
    beforeTest {
        fakeRepository.clear()
    }
    
    test("successfully updates metrics with valid positive values") {
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
        
        val savedMetrics = fakeRepository.getMetrics(chatId)
        savedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 20,
            lastResponseTokens = 30,
            totalTokens = 150 // 100 + 20 + 30
        )
    }
    
    test("successfully updates metrics with zero tokens") {
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
        
        val savedMetrics = fakeRepository.getMetrics(chatId)
        savedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 0,
            lastResponseTokens = 0,
            totalTokens = 0
        )
    }
    
    test("successfully updates metrics when current total is zero") {
        val chatId = "chat789"
        val currentTotalTokens = 0
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 300L,
            promptTokens = 15,
            completionTokens = 25,
            totalTokens = 40,
            costUsd = 0.0008
        )
        
        useCase(chatId, currentTotalTokens, responseMetrics)
        
        val savedMetrics = fakeRepository.getMetrics(chatId)
        savedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 15,
            lastResponseTokens = 25,
            totalTokens = 40 // 0 + 15 + 25
        )
    }
    
    test("successfully updates metrics when response tokens are zero") {
        val chatId = "chat999"
        val currentTotalTokens = 50
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 100L,
            promptTokens = 10,
            completionTokens = 0,
            totalTokens = 10,
            costUsd = 0.0002
        )
        
        useCase(chatId, currentTotalTokens, responseMetrics)
        
        val savedMetrics = fakeRepository.getMetrics(chatId)
        savedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 10,
            lastResponseTokens = 0,
            totalTokens = 60 // 50 + 10 + 0
        )
    }
    
    test("throws IllegalArgumentException when currentTotalTokens is negative") {
        val chatId = "chatNeg"
        val currentTotalTokens = -5
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 400L,
            promptTokens = 10,
            completionTokens = 20,
            totalTokens = 30,
            costUsd = 0.0006
        )
        
        val exception = shouldThrow<IllegalArgumentException> {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }
        
        exception.message shouldBe "currentTotalTokens must be non-negative"
    }
    
    test("throws IllegalArgumentException when promptTokens is negative") {
        val chatId = "chatNegPrompt"
        val currentTotalTokens = 100
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 400L,
            promptTokens = -5,
            completionTokens = 20,
            totalTokens = 15,
            costUsd = 0.0006
        )
        
        val exception = shouldThrow<IllegalArgumentException> {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }
        
        exception.message shouldBe "promptTokens must be non-negative"
    }
    
    test("throws IllegalArgumentException when completionTokens is negative") {
        val chatId = "chatNegCompletion"
        val currentTotalTokens = 100
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 400L,
            promptTokens = 10,
            completionTokens = -3,
            totalTokens = 7,
            costUsd = 0.0006
        )
        
        val exception = shouldThrow<IllegalArgumentException> {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }
        
        exception.message shouldBe "completionTokens must be non-negative"
    }
    
    test("overwrites existing metrics for same chatId") {
        val chatId = "sameChat"
        
        // First update
        useCase(chatId, 0, ResponseMetrics(
            responseTimeMs = 100L,
            promptTokens = 10,
            completionTokens = 20,
            totalTokens = 30,
            costUsd = 0.0003
        ))
        
        // Second update with new current total (should be 30 from first update)
        useCase(chatId, 30, ResponseMetrics(
            responseTimeMs = 150L,
            promptTokens = 5,
            completionTokens = 15,
            totalTokens = 20,
            costUsd = 0.0002
        ))
        
        val savedMetrics = fakeRepository.getMetrics(chatId)
        savedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 5,
            lastResponseTokens = 15,
            totalTokens = 50 // 30 + 5 + 15
        )
    }
})