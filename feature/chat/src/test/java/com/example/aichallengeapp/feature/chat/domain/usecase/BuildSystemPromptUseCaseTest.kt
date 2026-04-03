package com.example.aichallengeapp.feature.chat.domain.usecase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeGreaterThan

import com.example.aichallengeapp.core.database.domain.model.Constraint
import com.example.aichallengeapp.feature.chat.domain.PromptTemplates
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
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
            taskMemory = "Previous context",
            supportEnabled = true,
            maxLength = null
        )
        result shouldContain "## Память задачи"
        result shouldContain "Previous context"
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

    test("includes multiple constraints with correct numbering") {
        val constraints = listOf(
            Constraint("Constraint1", "Description1", "regex1", true),
            Constraint("Constraint2", "Description2", "regex2", false)
        )
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = constraints,
            taskMemory = null,
            supportEnabled = true,
            maxLength = null
        )
        result shouldContain "1. Constraint1: Description1"
        result shouldContain "2. Constraint2: Description2"
    }

    test("truncates result when maxLength is specified and result exceeds it") {
        val longPrefix = "A".repeat(100)
        val result = useCase(
            globalPrefix = longPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = 50
        )
        result.length shouldBe 50
        result shouldContain PromptTemplates.BASE_SYSTEM_PROMPT.substring(0, 20)
    }

    test("does not truncate when maxLength is null") {
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
    }

    test("does not truncate when result length is less than maxLength") {
        val result = useCase(
            globalPrefix = "Short",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = 1000
        )
        val fullResult = useCase(
            globalPrefix = "Short",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = null
        )
        result shouldBe fullResult
    }

    test("handles maxLength equal to result length") {
        val fullResult = useCase(
            globalPrefix = "Test",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = null
        )
        val result = useCase(
            globalPrefix = "Test",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = fullResult.length
        )
        result shouldBe fullResult
    }

    test("handles maxLength of zero") {
        val result = useCase(
            globalPrefix = "Test",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = true,
            maxLength = 0
        )
        result shouldBe ""
    }

    test("combines all components in correct order") {
        val constraints = listOf(
            Constraint("Test", "Description", "regex", true)
        )
        val result = useCase(
            globalPrefix = "Prefix",
            chatPrompt = "ChatPrompt",
            constraints = constraints,
            taskMemory = "TaskMemory",
            supportEnabled = true,
            maxLength = null
        )
        
        val parts = result.split("\n\n")
        parts shouldHaveSize 5
        parts[0] shouldBe PromptTemplates.SUPPORT_SYSTEM_PROMPT
        parts[1] shouldBe PromptTemplates.BASE_SYSTEM_PROMPT
        parts[2] shouldBe "Prefix"
        parts[3] shouldBe "## Память задачи\nTaskMemory"
        parts[4] shouldContain "ChatPrompt"
        parts[4] shouldContain "ВАЖНО"
    }
})