package com.example.aichallengeapp.feature.chat.domain.usecase

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
        val shortPrefix = "Short"
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
        result.length shouldBeLessThan 1000
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

    test("handles maxLength of zero by returning empty string") {
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

    test("handles maxLength equal to exact result length") {
        val testContent = "Test content"
        val result = useCase(
            globalPrefix = testContent,
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null,
            supportEnabled = false,
            maxLength = testContent.length + PromptTemplates.BASE_SYSTEM_PROMPT.length + 2
        )
        result shouldContain testContent
        result shouldContain PromptTemplates.BASE_SYSTEM_PROMPT
    }

    test("includes multiple constraints with correct numbering") {
        val constraints = listOf(
            Constraint("Constraint 1", "Description 1", "pattern1", true),
            Constraint("Constraint 2", "Description 2", "pattern2", false),
            Constraint("Constraint 3", "Description 3", "pattern3", true)
        )
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = constraints,
            taskMemory = null,
            supportEnabled = true,
            maxLength = null
        )
        result shouldContain "1. Constraint 1: Description 1"
        result shouldContain "2. Constraint 2: Description 2"
        result shouldContain "3. Constraint 3: Description 3"
    }

    test("orders parts correctly: support, base, prefix, memory, chat, constraints") {
        val result = useCase(
            globalPrefix = "Prefix",
            chatPrompt = "Chat",
            constraints = listOf(Constraint("C", "D", "R", true)),
            taskMemory = "Memory",
            supportEnabled = true,
            maxLength = null
        )
        val lines = result.split("\n\n")
        lines shouldHaveSize 6
        lines[0] shouldBe PromptTemplates.SUPPORT_SYSTEM_PROMPT
        lines[1] shouldBe PromptTemplates.BASE_SYSTEM_PROMPT
        lines[2] shouldBe "Prefix"
        lines[3] shouldBe "## Память задачи\nMemory"
        lines[4] shouldBe "Chat"
        lines[5] shouldContain "ВАЖНО"
    }
}
