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
        val shortText = "Short prefix"
        val result = useCase(
            globalPrefix = shortText,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = 1000
        )
        result shouldContain shortText
        result.length shouldBe shortText.length + PromptTemplates.BASE_SYSTEM_PROMPT.length + 2
    }

    test("truncates when result length exceeds maxLength") {
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

    test("truncates exactly at maxLength boundary") {
        val text = "Test prefix"
        val baseLength = PromptTemplates.BASE_SYSTEM_PROMPT.length + 2
        val totalLength = text.length + baseLength
        
        // Set maxLength to be exactly the total length
        val result = useCase(
            globalPrefix = text,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = totalLength
        )
        result shouldHaveLength totalLength
        result shouldContain text
    }

    test("handles all components together with maxLength") {
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
            maxLength = 200
        )
        
        result.length shouldBe 200
        result shouldContain "Prefix"
        result shouldContain "Chat prompt"
        result shouldContain "Task memory"
        result shouldContain "Rule 1"
        result shouldContain PromptTemplates.SUPPORT_SYSTEM_PROMPT
        result shouldContain PromptTemplates.BASE_SYSTEM_PROMPT
    }
})
