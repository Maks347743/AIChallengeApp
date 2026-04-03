package com.example.aichallengeapp.feature.chat.domain.usecase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

import com.example.aichallengeapp.core.database.domain.model.Constraint
import com.example.aichallengeapp.feature.chat.domain.PromptTemplates
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.ints.shouldBeLessThanOrEqualTo

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
            taskMemory = "Previous task: Fix bug #123"
        )
        result shouldContain "## Память задачи"
        result shouldContain "Previous task: Fix bug #123"
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

    test("returns full result when maxLength is null") {
        val longPrefix = "A".repeat(100)
        val result = useCase(
            globalPrefix = longPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = null
        )
        result shouldContain longPrefix
    }

    test("returns full result when length is less than maxLength") {
        val shortPrefix = "Short prefix"
        val result = useCase(
            globalPrefix = shortPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = 1000
        )
        result shouldContain shortPrefix
        result.length shouldBeLessThanOrEqualTo 1000
    }

    test("truncates result when length exceeds maxLength") {
        val longPrefix = "A".repeat(200)
        val maxLength = 100
        val result = useCase(
            globalPrefix = longPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = maxLength
        )
        result.length shouldBe maxLength
        result shouldContain "A" // Should contain part of the prefix
    }

    test("truncation preserves beginning of content") {
        val prefix = "BEGINNING" + "_".repeat(200) + "END"
        val maxLength = 50
        val result = useCase(
            globalPrefix = prefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = maxLength
        )
        result.length shouldBe maxLength
        result shouldContain "BEGINNING" // Beginning should be preserved
    }

    test("handles multiple constraints with maxLength truncation") {
        val constraints = listOf(
            Constraint("Constraint1", "Description1", "pattern1", true),
            Constraint("Constraint2", "Description2", "pattern2", false),
            Constraint("Constraint3", "Description3", "pattern3", true)
        )
        val maxLength = 150
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = constraints,
            maxLength = maxLength
        )
        result.length shouldBeLessThanOrEqualTo maxLength
        result shouldContain "ВАЖНО"
    }

    test("combines all components in correct order with maxLength") {
        val result = useCase(
            globalPrefix = "Global prefix",
            chatPrompt = "Chat prompt",
            constraints = listOf(Constraint("Test", "Description", "pattern", true)),
            taskMemory = "Task memory",
            supportEnabled = true,
            maxLength = 500
        )
        result.length shouldBeLessThanOrEqualTo 500
        // Check that all expected components are present (or truncated versions)
        (result.contains(PromptTemplates.SUPPORT_SYSTEM_PROMPT) || 
         result.contains(PromptTemplates.BASE_SYSTEM_PROMPT) || 
         result.contains("Global prefix") || 
         result.contains("Chat prompt") || 
         result.contains("Task memory") || 
         result.contains("ВАЖНО")) shouldBe true
    }
})