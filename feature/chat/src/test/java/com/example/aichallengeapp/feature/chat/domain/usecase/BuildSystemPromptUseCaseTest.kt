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

    test("truncates result when maxLength is specified and result exceeds it") {
        val longPrefix = "A".repeat(100)
        val result = useCase(
            globalPrefix = longPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = 50
        )
        result.length shouldBe 50
        result shouldContain "A"
    }

    test("does not truncate when maxLength is null") {
        val longPrefix = "A".repeat(100)
        val result = useCase(
            globalPrefix = longPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = null
        )
        result.length shouldBeGreaterThan 50
    }

    test("does not truncate when result length is less than maxLength") {
        val result = useCase(
            globalPrefix = "Short",
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = 1000
        )
        result.length shouldBeLessThan 1000
        result shouldContain "Short"
    }

    test("handles maxLength of zero") {
        val result = useCase(
            globalPrefix = "Test",
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = 0
        )
        result.length shouldBe 0
        result shouldBe ""
    }

    test("preserves all components when maxLength is large enough") {
        val constraints = listOf(
            Constraint("Rule1", "Description1", "pattern1", true),
            Constraint("Rule2", "Description2", "pattern2", false)
        )
        val result = useCase(
            globalPrefix = "Prefix",
            chatPrompt = "Chat prompt",
            constraints = constraints,
            taskMemory = "Memory",
            supportEnabled = true,
            maxLength = 10000
        )
        result shouldContain PromptTemplates.SUPPORT_SYSTEM_PROMPT
        result shouldContain PromptTemplates.BASE_SYSTEM_PROMPT
        result shouldContain "Prefix"
        result shouldContain "## Память задачи"
        result shouldContain "Memory"
        result shouldContain "Chat prompt"
        result shouldContain "ВАЖНО"
        result shouldContain "Rule1"
        result shouldContain "Rule2"
    }

    test("truncation preserves beginning of prompt when maxLength is small") {
        val result = useCase(
            globalPrefix = "Prefix",
            chatPrompt = "Chat",
            constraints = emptyList(),
            maxLength = 10
        )
        result.length shouldBe 10
        result shouldContain PromptTemplates.BASE_SYSTEM_PROMPT.substring(0, 10)
    }
})