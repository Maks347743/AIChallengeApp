package com.example.aichallengeapp.feature.chat.domain.usecase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual

import com.example.aichallengeapp.core.database.domain.model.Constraint
import com.example.aichallengeapp.feature.chat.domain.PromptTemplates
import io.kotest.core.spec.style.FunSpec
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
            taskMemory = "Previous task: fix bug",
            supportEnabled = true,
            maxLength = null
        )
        result shouldContain "## Память задачи"
        result shouldContain "Previous task: fix bug"
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
        val globalPrefix = "A" * 1000  // Create a long prefix
        val result = useCase(
            globalPrefix = globalPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = null
        )
        result shouldContain globalPrefix
        result.length shouldBeGreaterThan 1000
    }

    test("does not truncate when result length is less than maxLength") {
        val globalPrefix = "Short prefix"
        val result = useCase(
            globalPrefix = globalPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = 100000
        )
        result shouldContain globalPrefix
        result.length shouldBeLessThan 100000
    }

    test("truncates to exact maxLength when result exceeds maxLength") {
        val globalPrefix = "A" * 1000  // Create a long prefix
        val result = useCase(
            globalPrefix = globalPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = 50
        )
        result.length shouldBe 50
    }

    test("handles empty inputs with maxLength") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = 100
        )
        result shouldContain PromptTemplates.BASE_SYSTEM_PROMPT
        result shouldContain PromptTemplates.SUPPORT_SYSTEM_PROMPT
        result.length shouldBeLessThanOrEqual 100
    }

    test("maxLength zero returns empty string") {
        val result = useCase(
            globalPrefix = "Test",
            chatPrompt = "Test",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = 0
        )
        result shouldBe ""
        result.length shouldBe 0
    }

    test("maxLength exactly matches result length returns full result") {
        // First get the length of a known result
        val testResult = useCase(
            globalPrefix = "Test",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = null
        )
        val exactLength = testResult.length
        
        // Now call with that exact maxLength
        val result = useCase(
            globalPrefix = "Test",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = exactLength
        )
        result shouldBe testResult
        result.length shouldBe exactLength
    }
})