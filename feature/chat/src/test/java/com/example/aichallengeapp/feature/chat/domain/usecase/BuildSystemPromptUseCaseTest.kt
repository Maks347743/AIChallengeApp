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
        val longText = "A".repeat(100)
        val result = useCase(
            globalPrefix = longText,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = null
        )
        result shouldContain longText
        result.length shouldBe 100 + PromptTemplates.BASE_SYSTEM_PROMPT.length + 2
    }

    test("returns full result when length is less than maxLength") {
        val shortText = "Short"
        val result = useCase(
            globalPrefix = shortText,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = 1000
        )
        result shouldContain shortText
        result.length shouldBe shortText.length + PromptTemplates.BASE_SYSTEM_PROMPT.length + 2
    }

    test("truncates result when length exceeds maxLength") {
        val longText = "A".repeat(100)
        val maxLength = 50
        val result = useCase(
            globalPrefix = longText,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = maxLength
        )
        result shouldHaveLength maxLength
        result shouldContain "A"
    }

    test("handles maxLength equal to exact result length") {
        val text = "Exact length"
        val resultWithoutMax = useCase(
            globalPrefix = text,
            chatPrompt = "",
            constraints = emptyList()
        )
        val exactLength = resultWithoutMax.length
        
        val result = useCase(
            globalPrefix = text,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = exactLength
        )
        result shouldHaveLength exactLength
        result shouldBe resultWithoutMax
    }

    test("truncates at maxLength boundary when result is longer") {
        val prefix = "Prefix"
        val chat = "Chat prompt"
        val constraints = listOf(
            Constraint("C1", "Desc1", "type1", true),
            Constraint("C2", "Desc2", "type2", true)
        )
        
        val fullResult = useCase(
            globalPrefix = prefix,
            chatPrompt = chat,
            constraints = constraints,
            maxLength = null
        )
        
        val maxLength = 30
        val truncated = useCase(
            globalPrefix = prefix,
            chatPrompt = chat,
            constraints = constraints,
            maxLength = maxLength
        )
        
        truncated shouldHaveLength maxLength
        truncated shouldBe fullResult.take(maxLength)
    }

    test("includes support prompt when supportEnabled is true") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            supportEnabled = true
        )
        result shouldContain PromptTemplates.SUPPORT_SYSTEM_PROMPT
    }

    test("excludes support prompt when supportEnabled is false") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            supportEnabled = false
        )
        result shouldContain PromptTemplates.BASE_SYSTEM_PROMPT
        result shouldNotContain PromptTemplates.SUPPORT_SYSTEM_PROMPT
    }

    test("includes task memory when not null or blank") {
        val taskMemory = "Previous task context"
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = taskMemory
        )
        result shouldContain "## Память задачи"
        result shouldContain taskMemory
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
})
