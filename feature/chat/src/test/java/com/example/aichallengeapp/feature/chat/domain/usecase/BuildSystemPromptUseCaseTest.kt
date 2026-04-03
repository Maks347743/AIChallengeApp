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

    test("does not truncate when maxLength is null") {
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

    test("does not truncate when result length is less than maxLength") {
        val text = "Short text"
        val result = useCase(
            globalPrefix = text,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = 1000
        )
        result shouldContain text
        result.length shouldBe text.length + PromptTemplates.BASE_SYSTEM_PROMPT.length + 2
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
        result shouldNotContain PromptTemplates.BASE_SYSTEM_PROMPT
    }

    test("truncation preserves beginning of the prompt") {
        val prefix = "Important prefix"
        val chatPrompt = "Chat prompt"
        val constraints = listOf(
            Constraint("Constraint1", "Description1", "type1", true)
        )
        val maxLength = 30
        
        val result = useCase(
            globalPrefix = prefix,
            chatPrompt = chatPrompt,
            constraints = constraints,
            maxLength = maxLength
        )
        
        result shouldHaveLength maxLength
        result shouldContain prefix
        result shouldContain chatPrompt
    }

    test("includes task memory when provided") {
        val taskMemory = "Previous conversation summary"
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = taskMemory
        )
        result shouldContain "## Память задачи"
        result shouldContain taskMemory
    }

    test("excludes task memory when null or blank") {
        val result1 = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = null
        )
        val result2 = useCase(
            globalPrefix = "",
            chatPrompt = "",
            constraints = emptyList(),
            taskMemory = ""
        )
        
        result1 shouldNotContain "## Память задачи"
        result2 shouldNotContain "## Память задачи"
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
        result shouldNotContain PromptTemplates.SUPPORT_SYSTEM_PROMPT
    }
})