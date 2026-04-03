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
        val shortPrefix = "Short prefix"
        val result = useCase(
            globalPrefix = shortPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = 1000
        )
        result shouldContain shortPrefix
        result.length shouldBe shortPrefix.length + PromptTemplates.BASE_SYSTEM_PROMPT.length + 2
    }

    test("truncates when result length exceeds maxLength") {
        val longPrefix = "A".repeat(100)
        val maxLength = 50
        val result = useCase(
            globalPrefix = longPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = maxLength
        )
        result shouldHaveLength maxLength
        result shouldContain "A"
    }

    test("truncates exactly at maxLength boundary") {
        val prefix = "Prefix"
        val baseLength = PromptTemplates.BASE_SYSTEM_PROMPT.length + 2
        val totalLength = prefix.length + baseLength
        
        val result = useCase(
            globalPrefix = prefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = totalLength
        )
        result shouldHaveLength totalLength
        result shouldContain prefix
        result shouldContain PromptTemplates.BASE_SYSTEM_PROMPT
    }

    test("handles empty result with maxLength") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = 10
        )
        result shouldHaveLength PromptTemplates.BASE_SYSTEM_PROMPT.length
        result shouldContain PromptTemplates.BASE_SYSTEM_PROMPT
    }
})