package com.example.aichallengeapp.feature.chat.domain.usecase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

import com.example.aichallengeapp.core.database.domain.model.Constraint
import com.example.aichallengeapp.feature.chat.domain.PromptTemplates
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.ints.shouldBeLessThanOrEqualToTo

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
            taskMemory = "Previous context"
        )
        result shouldContain "## Память задачи"
        result shouldContain "Previous context"
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
        result.length shouldBeLessThanOrEqualToTo 1000
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
        val prefix = "BEGINNING"
        val suffix = "END"
        val middle = "M".repeat(200)
        val chatPrompt = "$prefix$middle$suffix"
        val maxLength = 50
        
        val result = useCase(
            globalPrefix = "",
            chatPrompt = chatPrompt,
            constraints = emptyList(),
            maxLength = maxLength
        )
        
        result.length shouldBe maxLength
        result shouldContain "BEGINNING" // Should preserve the beginning
    }

    test("handles multiple constraints with maxLength truncation") {
        val constraints = listOf(
            Constraint("Constraint1", "Description1", "pattern1", true),
            Constraint("Constraint2", "Description2", "pattern2", false),
            Constraint("Constraint3", "Description3", "pattern3", true)
        )
        
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = constraints,
            maxLength = 150
        )
        
        result.length shouldBeLessThanOrEqualToTo 150
        result shouldContain "ВАЖНО"
    }

    test("combines all components correctly with maxLength") {
        val result = useCase(
            globalPrefix = "Global prefix",
            chatPrompt = "Chat prompt",
            constraints = listOf(Constraint("Test", "Description", "pattern", true)),
            taskMemory = "Task memory",
            supportEnabled = true,
            maxLength = 500
        )
        
        result.length shouldBeLessThanOrEqualToTo 500
        result shouldContain PromptTemplates.SUPPORT_SYSTEM_PROMPT
        result shouldContain PromptTemplates.BASE_SYSTEM_PROMPT
        result shouldContain "Global prefix"
        result shouldContain "## Память задачи"
        result shouldContain "Task memory"
        result shouldContain "Chat prompt"
        result shouldContain "ВАЖНО"
        result shouldContain "Test"
    }
})