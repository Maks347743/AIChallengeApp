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

    test("truncates result when maxLength is specified and result exceeds it") {
        val longText = "A".repeat(100)
        val result = useCase(
            globalPrefix = longText,
            chatPrompt = longText,
            constraints = emptyList(),
            maxLength = 50
        )
        result shouldHaveLength 50
    }

    test("does not truncate when maxLength is null") {
        val longText = "A".repeat(100)
        val result = useCase(
            globalPrefix = longText,
            chatPrompt = longText,
            constraints = emptyList(),
            maxLength = null
        )
        result.length shouldBe 200 + PromptTemplates.BASE_SYSTEM_PROMPT.length + 4 // 4 for separators
    }

    test("does not truncate when result length is less than maxLength") {
        val result = useCase(
            globalPrefix = "short",
            chatPrompt = "text",
            constraints = emptyList(),
            maxLength = 1000
        )
        result shouldContain "short"
        result shouldContain "text"
        result shouldContain PromptTemplates.BASE_SYSTEM_PROMPT
    }

    test("truncation preserves beginning of the string") {
        val prefix = "START"
        val suffix = "END"
        val result = useCase(
            globalPrefix = prefix,
            chatPrompt = suffix,
            constraints = emptyList(),
            maxLength = prefix.length + 1
        )
        result shouldContain prefix
        result shouldNotContain suffix
        result shouldHaveLength (prefix.length + 1)
    }

    test("includes task memory when not null or blank") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = "Remember previous conversation"
        )
        result shouldContain "## Память задачи"
        result shouldContain "Remember previous conversation"
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
            taskMemory = "   "
        )
        result shouldNotContain "## Память задачи"
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
})