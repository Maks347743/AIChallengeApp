package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.Constraint
import com.example.aichallengeapp.feature.chat.domain.PromptTemplates
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldHaveLength
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
            taskMemory = "Previous conversation summary"
        )
        result shouldContain "## Память задачи"
        result shouldContain "Previous conversation summary"
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
        val longPrefix = "A".repeat(100)
        val result = useCase(
            globalPrefix = longPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = null
        )
        result shouldContain longPrefix
        result.length shouldBe 100 + PromptTemplates.BASE_SYSTEM_PROMPT.length + 2
    }

    test("does not truncate when result length is less than maxLength") {
        val result = useCase(
            globalPrefix = "Short prefix",
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = 1000
        )
        result shouldContain "Short prefix"
        result shouldContain PromptTemplates.BASE_SYSTEM_PROMPT
        result.length shouldBeLessThan 1000
    }

    test("truncates when result length exceeds maxLength") {
        val longPrefix = "A".repeat(200)
        val result = useCase(
            globalPrefix = longPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = 100
        )
        result shouldHaveLength 100
        result shouldContain "A" // Should contain part of the prefix
        result shouldContain PromptTemplates.BASE_SYSTEM_PROMPT.take(100 - longPrefix.take(100).length)
    }

    test("handles empty maxLength (zero) by returning empty string") {
        val result = useCase(
            globalPrefix = "Some content",
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = 0
        )
        result shouldHaveLength 0
        result shouldBe ""
    }

    test("preserves all components when not truncated") {
        val constraints = listOf(
            Constraint("Rule 1", "Description 1", "type1", true),
            Constraint("Rule 2", "Description 2", "type2", true)
        )
        val result = useCase(
            globalPrefix = "Prefix",
            chatPrompt = "Chat prompt",
            constraints = constraints,
            taskMemory = "Task memory",
            supportEnabled = true,
            maxLength = 5000
        )
        
        result shouldContain PromptTemplates.SUPPORT_SYSTEM_PROMPT
        result shouldContain PromptTemplates.BASE_SYSTEM_PROMPT
        result shouldContain "Prefix"
        result shouldContain "## Память задачи"
        result shouldContain "Task memory"
        result shouldContain "Chat prompt"
        result shouldContain "ВАЖНО"
        result shouldContain "Rule 1"
        result shouldContain "Rule 2"
    }
})