package com.example.aichallengeapp.feature.chat.domain.usecase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.string.shouldContain

import com.example.aichallengeapp.core.database.domain.model.ChatMetrics
import com.example.aichallengeapp.core.database.domain.model.ResponseMetrics
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UpdateMetricsUseCaseTest : FunSpec({

    test("successfully updates metrics with valid positive values") {
        class FakeMetricsRepository : com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository {
            var savedMetrics: ChatMetrics? = null
            override fun observeMetrics(chatId: String) = TODO("Not needed for this test")
            override suspend fun upsertMetrics(metrics: ChatMetrics) {
                savedMetrics = metrics
            }
            override suspend fun deleteMetrics(chatId: String) = TODO("Not needed for this test")
        }

        val fakeRepo = FakeMetricsRepository()
        val useCase = UpdateMetricsUseCase(fakeRepo)
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 100L,
            promptTokens = 50,
            completionTokens = 30,
            totalTokens = 80,
            costUsd = 0.001
        )

        useCase("chat123", 200, responseMetrics)

        fakeRepo.savedMetrics shouldBe ChatMetrics(
            chatId = "chat123",
            lastRequestTokens = 50,
            lastResponseTokens = 30,
            totalTokens = 280 // 200 + 50 + 30
        )
    }

    test("successfully updates metrics with zero token values") {
        class FakeMetricsRepository : com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository {
            var savedMetrics: ChatMetrics? = null
            override fun observeMetrics(chatId: String) = TODO("Not needed for this test")
            override suspend fun upsertMetrics(metrics: ChatMetrics) {
                savedMetrics = metrics
            }
            override suspend fun deleteMetrics(chatId: String) = TODO("Not needed for this test")
        }

        val fakeRepo = FakeMetricsRepository()
        val useCase = UpdateMetricsUseCase(fakeRepo)
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 0L,
            promptTokens = 0,
            completionTokens = 0,
            totalTokens = 0,
            costUsd = 0.0
        )

        useCase("chat456", 0, responseMetrics)

        fakeRepo.savedMetrics shouldBe ChatMetrics(
            chatId = "chat456",
            lastRequestTokens = 0,
            lastResponseTokens = 0,
            totalTokens = 0 // 0 + 0 + 0
        )
    }

    test("throws exception when currentTotalTokens is negative") {
        class FakeMetricsRepository : com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository {
            override fun observeMetrics(chatId: String) = TODO("Not needed for this test")
            override suspend fun upsertMetrics(metrics: ChatMetrics) = TODO("Not needed for this test")
            override suspend fun deleteMetrics(chatId: String) = TODO("Not needed for this test")
        }

        val fakeRepo = FakeMetricsRepository()
        val useCase = UpdateMetricsUseCase(fakeRepo)
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 100L,
            promptTokens = 10,
            completionTokens = 5,
            totalTokens = 15,
            costUsd = 0.0005
        )

        val exception = shouldThrow<IllegalArgumentException> {
            useCase("chat789", -1, responseMetrics)
        }
        exception.message shouldContain "currentTotalTokens must be non-negative"
    }

    test("throws exception when promptTokens is negative") {
        class FakeMetricsRepository : com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository {
            override fun observeMetrics(chatId: String) = TODO("Not needed for this test")
            override suspend fun upsertMetrics(metrics: ChatMetrics) = TODO("Not needed for this test")
            override suspend fun deleteMetrics(chatId: String) = TODO("Not needed for this test")
        }

        val fakeRepo = FakeMetricsRepository()
        val useCase = UpdateMetricsUseCase(fakeRepo)
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 100L,
            promptTokens = -5,
            completionTokens = 10,
            totalTokens = 5,
            costUsd = 0.0003
        )

        val exception = shouldThrow<IllegalArgumentException> {
            useCase("chat999", 100, responseMetrics)
        }
        exception.message shouldContain "promptTokens must be non-negative"
    }

    test("throws exception when completionTokens is negative") {
        class FakeMetricsRepository : com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository {
            override fun observeMetrics(chatId: String) = TODO("Not needed for this test")
            override suspend fun upsertMetrics(metrics: ChatMetrics) = TODO("Not needed for this test")
            override suspend fun deleteMetrics(chatId: String) = TODO("Not needed for this test")
        }

        val fakeRepo = FakeMetricsRepository()
        val useCase = UpdateMetricsUseCase(fakeRepo)
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 100L,
            promptTokens = 20,
            completionTokens = -3,
            totalTokens = 17,
            costUsd = 0.0004
        )

        val exception = shouldThrow<IllegalArgumentException> {
            useCase("chat111", 50, responseMetrics)
        }
        exception.message shouldContain "completionTokens must be non-negative"
    }

    test("correctly calculates total with large token values") {
        class FakeMetricsRepository : com.example.aichallengeapp.core.database.domain.repository.ChatMetricsRepository {
            var savedMetrics: ChatMetrics? = null
            override fun observeMetrics(chatId: String) = TODO("Not needed for this test")
            override suspend fun upsertMetrics(metrics: ChatMetrics) {
                savedMetrics = metrics
            }
            override suspend fun deleteMetrics(chatId: String) = TODO("Not needed for this test")
        }

        val fakeRepo = FakeMetricsRepository()
        val useCase = UpdateMetricsUseCase(fakeRepo)
        val responseMetrics = ResponseMetrics(
            responseTimeMs = 500L,
            promptTokens = 10000,
            completionTokens = 5000,
            totalTokens = 15000,
            costUsd = 0.1
        )

        useCase("chat-large", 25000, responseMetrics)

        fakeRepo.savedMetrics shouldBe ChatMetrics(
            chatId = "chat-large",
            lastRequestTokens = 10000,
            lastResponseTokens = 5000,
            totalTokens = 40000 // 25000 + 10000 + 5000
        )
    }
})