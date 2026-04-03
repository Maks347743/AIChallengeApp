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
            promptTokens = 50,
            completionTokens = 30,
            totalTokens = 80,
            costUsd = 0.001
        )

        useCase(chatId, currentTotalTokens, responseMetrics)

        fakeRepository.savedMetrics.shouldNotBeNull()
        fakeRepository.savedMetrics!!.chatId shouldBe chatId
        fakeRepository.savedMetrics!!.lastRequestTokens shouldBe responseMetrics.promptTokens
        fakeRepository.savedMetrics!!.lastResponseTokens shouldBe responseMetrics.completionTokens
        fakeRepository.savedMetrics!!.totalTokens shouldBe 180 // 100 + 50 + 30
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

        fakeRepository.savedMetrics.shouldNotBeNull()
        fakeRepository.savedMetrics!!.totalTokens shouldBe 15 // 0 + 10 + 5
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

        fakeRepository.savedMetrics.shouldNotBeNull()
        fakeRepository.savedMetrics!!.totalTokens shouldBe 200 // 200 + 0 + 0
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

        val exception = kotlin.runCatching {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }.exceptionOrNull()

        exception.shouldBeInstanceOf<IllegalArgumentException>()
        exception.message shouldBe "currentTotalTokens must be non-negative"
    }

    test("should throw IllegalArgumentException when promptTokens is negative") {
        val chatId = "chat888"
        val currentTotalTokens = 100
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 200L,
            promptTokens = -5,
            completionTokens = 10,
            totalTokens = 5,
            costUsd = 0.0005
        )

        val exception = kotlin.runCatching {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }.exceptionOrNull()

        exception.shouldBeInstanceOf<IllegalArgumentException>()
        exception.message shouldBe "promptTokens must be non-negative"
    }

    test("should throw IllegalArgumentException when completionTokens is negative") {
        val chatId = "chat777"
        val currentTotalTokens = 100
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 200L,
            promptTokens = 10,
            completionTokens = -3,
            totalTokens = 7,
            costUsd = 0.0005
        )

        val exception = kotlin.runCatching {
            useCase(chatId, currentTotalTokens, responseMetrics)
        }.exceptionOrNull()

        exception.shouldBeInstanceOf<IllegalArgumentException>()
        exception.message shouldBe "completionTokens must be non-negative"
    }

    test("should correctly update metrics for same chatId multiple times") {
        val chatId = "same-chat"
        val responseMetrics1 = ResponseMetrics(
            responseTimeMs = 100L,
            promptTokens = 20,
            completionTokens = 10,
            totalTokens = 30,
            costUsd = 0.001
        )
        val responseMetrics2 = ResponseMetrics(
            responseTimeMs = 150L,
            promptTokens = 15,
            completionTokens = 8,
            totalTokens = 23,
            costUsd = 0.0008
        )

        // First call
        useCase(chatId, 0, responseMetrics1)
        fakeRepository.savedMetrics!!.totalTokens shouldBe 30 // 0 + 20 + 10
        
        // Second call with updated total
        useCase(chatId, 30, responseMetrics2)
        fakeRepository.savedMetrics!!.totalTokens shouldBe 53 // 30 + 15 + 8
        fakeRepository.savedMetrics!!.lastRequestTokens shouldBe responseMetrics2.promptTokens
        fakeRepository.savedMetrics!!.lastResponseTokens shouldBe responseMetrics2.completionTokens
    }
})