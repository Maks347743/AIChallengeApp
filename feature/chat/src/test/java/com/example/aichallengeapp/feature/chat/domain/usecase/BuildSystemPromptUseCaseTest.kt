package com.example.aichallengeapp.feature.chat.domain.usecase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

import com.example.aichallengeapp.core.database.domain.model.Constraint
import com.example.aichallengeapp.feature.chat.domain.PromptTemplates
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
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

    test("does not truncate when maxLength is null") {
        val longPrefix = "A".repeat(1000)
        val result = useCase(
            globalPrefix = longPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = null
        )
        result shouldContain longPrefix
        result.length shouldBeGreaterThan 1000
    }

    test("does not truncate when result length is less than maxLength") {
        val prefix = "Short prefix"
        val result = useCase(
            globalPrefix = prefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = 100000
        )
        result shouldContain prefix
        result.length shouldBeLessThanOrEqual 100000
    }

    test("does not truncate when using large maxLength (100000)") {
        val uniquePrefix = "UNIQUE_GLOBAL_PREFIX_12345"
        val result = useCase(
            globalPrefix = uniquePrefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = 100000
        )
        result shouldContain uniquePrefix
    }

    test("truncates to exact maxLength when result exceeds maxLength") {
        val longPrefix = "A".repeat(100)
        val result = useCase(
            globalPrefix = longPrefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = 50
        )
        result.length shouldBe 50
    }

    test("truncates when result exactly equals maxLength") {
        val prefix = "A".repeat(10)
        val result = useCase(
            globalPrefix = prefix,
            chatPrompt = "",
            constraints = emptyList(),
            maxLength = result.length
        )
        result shouldContain prefix
    }

    test("handles all components together with truncation") {
        val constraints = listOf(
            Constraint("Constraint1", "Description1", "pattern1", true),
            Constraint("Constraint2", "Description2", "pattern2", false)
        )
        val result = useCase(
            globalPrefix = "Global prefix",
            chatPrompt = "Chat prompt",
            constraints = constraints,
            taskMemory = "Task memory content",
            supportEnabled = true,
            maxLength = 100
        )
        result.length shouldBe 100
    }
})