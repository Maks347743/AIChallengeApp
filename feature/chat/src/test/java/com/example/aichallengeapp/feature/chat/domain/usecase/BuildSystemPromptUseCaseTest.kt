package com.example.aichallengeapp.feature.chat.domain.usecase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

import com.example.aichallengeapp.core.database.domain.model.Constraint
import com.example.aichallengeapp.feature.chat.domain.PromptTemplates
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class BuildSystemPromptUseCaseTest : FunSpec({

    val useCase = BuildSystemPromptUseCase()

    test("always includes base system prompt") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = null
        )
        result shouldContain PromptTemplates.BASE_SYSTEM_PROMPT
    }

    test("includes global prefix when not blank") {
        val result = useCase(
            globalPrefix = "You are a helpful assistant",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = null
        )
        result shouldContain "You are a helpful assistant"
    }

    test("excludes global prefix when blank") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "custom prompt",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = null
        )
        result shouldContain "custom prompt"
    }

    test("includes constraints block") {
        val constraints = listOf(
            Constraint("No swearing", "Do not swear", "bad", true)
        )
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = constraints,
            taskMemory = null,
            supportEnabled = true,
            maxLength = null
        )
        result shouldContain "ВАЖНО"
        result shouldContain "No swearing"
    }

    test("includes support system prompt when supportEnabled is true") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = null
        )
        result shouldContain PromptTemplates.SUPPORT_SYSTEM_PROMPT
    }

    test("excludes support system prompt when supportEnabled is false") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = false,
            maxLength = null
        )
        result shouldNotContain PromptTemplates.SUPPORT_SYSTEM_PROMPT
    }

    test("includes task memory when not null or blank") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = "Previous task: fix bug #123",
            supportEnabled = true,
            maxLength = null
        )
        result shouldContain "## Память задачи"
        result shouldContain "Previous task: fix bug #123"
    }

    test("excludes task memory when null") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = null
        )
        result shouldNotContain "## Память задачи"
    }

    test("excludes task memory when blank") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = "   ",
            supportEnabled = true,
            maxLength = null
        )
        result shouldNotContain "## Память задачи"
    }

    test("does not truncate when maxLength is null") {
        val longPrefix = "A".repeat(1000)
        val result = useCase(
            globalPrefix = longPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = null
        )
        result shouldContain longPrefix
    }

    test("does not truncate when result length is less than or equal to maxLength") {
        val uniquePrefix = "UNIQUE_PREFIX_123"
        val result = useCase(
            globalPrefix = uniquePrefix,
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = 100000
        )
        result shouldContain uniquePrefix
        result.length shouldBeLessThanOrEqual 100000
    }

    test("truncates to exact maxLength when result exceeds maxLength") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = 50
        )
        result.length shouldBe 50
    }

    test("truncation preserves beginning of content") {
        val longPrefix = "A".repeat(1000)
        val result = useCase(
            globalPrefix = longPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = 100
        )
        result.length shouldBe 100
        // The truncated result should start with the same beginning as the untruncated one
        val fullResult = useCase(
            globalPrefix = longPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = null
        )
        result shouldBe fullResult.take(100)
    }
})