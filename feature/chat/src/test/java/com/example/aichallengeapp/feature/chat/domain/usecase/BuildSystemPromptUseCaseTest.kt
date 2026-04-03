package com.example.aichallengeapp.feature.chat.domain.usecase
import io.kotest.matchers.ints.shouldBeGreaterThan

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
            taskMemory = "Previous task: Fix bug #123",
            supportEnabled = true,
            maxLength = null
        )
        result shouldContain "## Память задачи"
        result shouldContain "Previous task: Fix bug #123"
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

    test("returns full result when maxLength is null") {
        val longPrefix = "A".repeat(100)
        val result = useCase(
            globalPrefix = longPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = null
        )
        result.length shouldBeGreaterThan 100
        result shouldContain longPrefix
    }

    test("returns full result when result length is less than maxLength") {
        val shortPrefix = "Short prefix"
        val result = useCase(
            globalPrefix = shortPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = 1000
        )
        result shouldContain shortPrefix
        result shouldContain PromptTemplates.BASE_SYSTEM_PROMPT
        result shouldContain PromptTemplates.SUPPORT_SYSTEM_PROMPT
    }

    test("truncates result when result length exceeds maxLength") {
        val longPrefix = "A".repeat(200)
        val result = useCase(
            globalPrefix = longPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = 100
        )
        result.length shouldBe 100
        result shouldBe longPrefix.take(100)
    }

    test("truncates result exactly at maxLength boundary") {
        val exactLengthText = "A".repeat(150)
        val result = useCase(
            globalPrefix = exactLengthText,
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = 150
        )
        result.length shouldBe 150
        result shouldBe exactLengthText
    }

    test("handles maxLength of zero by returning empty string") {
        val result = useCase(
            globalPrefix = "Some text",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = 0
        )
        result shouldBe ""
    }

    test("preserves order of components when not truncated") {
        val result = useCase(
            globalPrefix = "Prefix",
            chatPrompt = "Chat prompt",
            constraints = listOf(Constraint("C1", "Desc1", "regex1", true)),
            taskMemory = "Memory",
            supportEnabled = true,
            maxLength = null
        )
        val lines = result.lines()
        lines[0] shouldContain PromptTemplates.SUPPORT_SYSTEM_PROMPT.substringBefore("\n")
        lines shouldContain "Prefix"
        lines shouldContain "## Память задачи"
        lines shouldContain "Chat prompt"
        lines shouldContain "ВАЖНО"
    }

    test("truncation preserves beginning of result") {
        val longPrefix = "START_".repeat(50)
        val result = useCase(
            globalPrefix = longPrefix,
            chatPrompt = "END_TEXT",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = 100
        )
        result.length shouldBe 100
        result shouldContain "START_"
        result shouldNotContain "END_TEXT"
    }
})