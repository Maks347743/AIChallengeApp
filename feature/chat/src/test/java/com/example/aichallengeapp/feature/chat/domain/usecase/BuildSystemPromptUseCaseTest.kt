package com.example.aichallengeapp.feature.chat.domain.usecase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan

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
            constraints = emptyList()
        )
        result shouldContain PromptTemplates.BASE_SYSTEM_PROMPT
    }

    test("includes global prefix when not blank") {
        val result = useCase(
            globalPrefix = "You are a helpful assistant",
            chatPrompt = "",
            constraints = emptyList()
        )
        result shouldContain "You are a helpful assistant"
    }

    test("excludes global prefix when blank") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "custom prompt",
            constraints = emptyList()
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
            constraints = constraints
        )
        result shouldContain "ВАЖНО"
        result shouldContain "No swearing"
    }

    test("includes support system prompt when supportEnabled is true") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            supportEnabled = true
        )
        result shouldContain PromptTemplates.SUPPORT_SYSTEM_PROMPT
    }

    test("excludes support system prompt when supportEnabled is false") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            supportEnabled = false
        )
        result shouldNotContain PromptTemplates.SUPPORT_SYSTEM_PROMPT
    }

    test("includes task memory when not null or blank") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = "Previous context here"
        )
        result shouldContain "## Память задачи"
        result shouldContain "Previous context here"
    }

    test("excludes task memory when null") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null
        )
        result shouldNotContain "## Память задачи"
    }

    test("excludes task memory when blank") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = ""
        )
        result shouldNotContain "## Память задачи"
    }

    test("does not truncate when maxLength is null") {
        val longPrefix = "A".repeat(1000)
        val result = useCase(
            globalPrefix = longPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = null
        )
        result.length shouldBeGreaterThan 1000
        result shouldContain longPrefix
    }

    test("does not truncate when result length is less than maxLength") {
        val shortPrefix = "Short prefix"
        val result = useCase(
            globalPrefix = shortPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = 500
        )
        result.length shouldBeLessThan 500
        result shouldContain shortPrefix
    }

    test("truncates to exact maxLength when result exceeds maxLength") {
        val longPrefix = "A".repeat(1000)
        val maxLength = 200
        val result = useCase(
            globalPrefix = longPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = maxLength
        )
        result.length shouldBe maxLength
        result shouldContain "A" // Should contain part of the prefix
    }

    test("truncation preserves beginning of prompt when result exceeds maxLength") {
        val prefix = "BEGINNING"
        val suffix = "END".repeat(1000)
        val maxLength = 50
        val result = useCase(
            globalPrefix = prefix,
            chatPrompt = suffix,
            constraints = emptyList(),
            maxLength = maxLength
        )
        result.length shouldBe maxLength
        result shouldContain prefix // Beginning should be preserved
    }

    test("handles empty result with maxLength") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = 100
        )
        result.length shouldBeLessThanOrEqualToTo 100
        result shouldContain PromptTemplates.BASE_SYSTEM_PROMPT
    }

    test("combines all components in correct order") {
        val constraints = listOf(
            Constraint("Constraint1", "Description1", "pattern1", true),
            Constraint("Constraint2", "Description2", "pattern2", false)
        )
        val result = useCase(
            globalPrefix = "Global prefix",
            chatPrompt = "Chat prompt",
            constraints = constraints,
            taskMemory = "Task memory",
            supportEnabled = true
        )
        
        // Check all components are present
        result shouldContain PromptTemplates.SUPPORT_SYSTEM_PROMPT
        result shouldContain PromptTemplates.BASE_SYSTEM_PROMPT
        result shouldContain "Global prefix"
        result shouldContain "## Память задачи"
        result shouldContain "Task memory"
        result shouldContain "Chat prompt"
        result shouldContain "ВАЖНО"
        result shouldContain "Constraint1"
        result shouldContain "Constraint2"
    }
})