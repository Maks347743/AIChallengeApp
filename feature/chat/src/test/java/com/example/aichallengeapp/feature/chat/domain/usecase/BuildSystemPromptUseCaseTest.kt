package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.Constraint
import com.example.aichallengeapp.feature.chat.domain.PromptTemplates
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.shouldBe

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

    test("returns full result when maxLength is null") {
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

    test("returns full result when length is less than maxLength") {
        val shortPrefix = "Short"
        val result = useCase(
            globalPrefix = shortPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = 1000
        )
        result shouldContain shortPrefix
        result.length shouldBe shortPrefix.length + PromptTemplates.BASE_SYSTEM_PROMPT.length + 2
    }

    test("truncates result when length exceeds maxLength") {
        val longPrefix = "A".repeat(100)
        val result = useCase(
            globalPrefix = longPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = 50
        )
        result shouldHaveLength 50
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

    test("maxLength zero returns empty string") {
        val result = useCase(
            globalPrefix = "Test",
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = 0
        )
        result shouldBe ""
        result shouldHaveLength 0
    }
})