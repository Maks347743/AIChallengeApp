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

    test("successfully upserts metrics with valid inputs") {
        class FakeChatMetricsRepository : ChatMetricsRepository {
            var savedMetrics: ChatMetrics? = null
            override fun observeMetrics(chatId: String) = flowOf<ChatMetrics?>(null)
            override suspend fun upsertMetrics(metrics: ChatMetrics) {
                savedMetrics = metrics
            }
            override suspend fun deleteMetrics(chatId: String) {}
        }

        val fakeRepo = FakeChatMetricsRepository()
        val useCase = UpdateMetricsUseCase(fakeRepo)
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

        fakeRepo.savedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 20,
            lastResponseTokens = 30,
            totalTokens = 150 // 100 + 20 + 30
        )
    }

    test("throws IllegalArgumentException when currentTotalTokens is negative") {
        class FakeChatMetricsRepository : ChatMetricsRepository {
            override fun observeMetrics(chatId: String) = flowOf<ChatMetrics?>(null)
            override suspend fun upsertMetrics(metrics: ChatMetrics) {}
            override suspend fun deleteMetrics(chatId: String) {}
        }

        val useCase = UpdateMetricsUseCase(FakeChatMetricsRepository())
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 500L,
            promptTokens = 20,
            completionTokens = 30,
            totalTokens = 50,
            costUsd = 0.001
        )

        val exception = shouldThrow<IllegalArgumentException> {
            useCase("chat123", -1, responseMetrics)
        }
        exception.message shouldContain "currentTotalTokens must be non-negative"
    }

    test("throws IllegalArgumentException when promptTokens is negative") {
        class FakeChatMetricsRepository : ChatMetricsRepository {
            override fun observeMetrics(chatId: String) = flowOf<ChatMetrics?>(null)
            override suspend fun upsertMetrics(metrics: ChatMetrics) {}
            override suspend fun deleteMetrics(chatId: String) {}
        }

        val useCase = UpdateMetricsUseCase(FakeChatMetricsRepository())
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 500L,
            promptTokens = -5,
            completionTokens = 30,
            totalTokens = 25,
            costUsd = 0.001
        )

        val exception = shouldThrow<IllegalArgumentException> {
            useCase("chat123", 100, responseMetrics)
        }
        exception.message shouldContain "promptTokens must be non-negative"
    }

    test("throws IllegalArgumentException when completionTokens is negative") {
        class FakeChatMetricsRepository : ChatMetricsRepository {
            override fun observeMetrics(chatId: String) = flowOf<ChatMetrics?>(null)
            override suspend fun upsertMetrics(metrics: ChatMetrics) {}
            override suspend fun deleteMetrics(chatId: String) {}
        }

        val useCase = UpdateMetricsUseCase(FakeChatMetricsRepository())
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 500L,
            promptTokens = 20,
            completionTokens = -10,
            totalTokens = 10,
            costUsd = 0.001
        )

        val exception = shouldThrow<IllegalArgumentException> {
            useCase("chat123", 100, responseMetrics)
        }
        exception.message shouldContain "completionTokens must be non-negative"
    }

    test("handles zero values correctly") {
        class FakeChatMetricsRepository : ChatMetricsRepository {
            var savedMetrics: ChatMetrics? = null
            override fun observeMetrics(chatId: String) = flowOf<ChatMetrics?>(null)
            override suspend fun upsertMetrics(metrics: ChatMetrics) {
                savedMetrics = metrics
            }
            override suspend fun deleteMetrics(chatId: String) {}
        }

        val fakeRepo = FakeChatMetricsRepository()
        val useCase = UpdateMetricsUseCase(fakeRepo)
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

        fakeRepo.savedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 0,
            lastResponseTokens = 0,
            totalTokens = 0 // 0 + 0 + 0
        )
    }

    test("correctly calculates new total with large values") {
        class FakeChatMetricsRepository : ChatMetricsRepository {
            var savedMetrics: ChatMetrics? = null
            override fun observeMetrics(chatId: String) = flowOf<ChatMetrics?>(null)
            override suspend fun upsertMetrics(metrics: ChatMetrics) {
                savedMetrics = metrics
            }
            override suspend fun deleteMetrics(chatId: String) {}
        }

        val fakeRepo = FakeChatMetricsRepository()
        val useCase = UpdateMetricsUseCase(fakeRepo)
        val chatId = "chat789"
        val currentTotalTokens = 10000
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 1000L,
            promptTokens = 2500,
            completionTokens = 1500,
            totalTokens = 4000,
            costUsd = 0.05
        )

        useCase(chatId, currentTotalTokens, responseMetrics)

        fakeRepo.savedMetrics shouldBe ChatMetrics(
            chatId = chatId,
            lastRequestTokens = 2500,
            lastResponseTokens = 1500,
            totalTokens = 14000 // 10000 + 2500 + 1500
        )
    }
})