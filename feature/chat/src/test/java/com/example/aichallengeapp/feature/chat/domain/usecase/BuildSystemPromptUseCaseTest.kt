package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.Constraint
import com.example.aichallengeapp.core.database.domain.model.TaskStage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class BuildSystemPromptUseCaseTest : FunSpec({

    val useCase = BuildSystemPromptUseCase()

    test("includes global prefix when not blank") {
        val result = useCase(
            globalPrefix = "You are a helpful assistant",
            chatPrompt = "",
            currentTaskStage = TaskStage.PLANNING,
            stageArtifacts = emptyMap(),
            constraints = emptyList(),
            currentTask = null
        )
        result shouldContain "You are a helpful assistant"
    }

    test("excludes global prefix when blank") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "custom prompt",
            currentTaskStage = TaskStage.PLANNING,
            stageArtifacts = emptyMap(),
            constraints = emptyList(),
            currentTask = null
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
            currentTaskStage = TaskStage.PLANNING,
            stageArtifacts = emptyMap(),
            constraints = constraints,
            currentTask = null
        )
        result shouldContain "ВАЖНО"
        result shouldContain "No swearing"
    }

    test("includes stage prompt for current stage") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            currentTaskStage = TaskStage.EXECUTION,
            stageArtifacts = emptyMap(),
            constraints = emptyList(),
            currentTask = null
        )
        result shouldContain "выполнить задачу"
    }

    test("includes artifacts from preceding stages only") {
        val artifacts = mapOf(
            TaskStage.PLANNING to "Plan artifact",
            TaskStage.EXECUTION to "Exec artifact"
        )
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            currentTaskStage = TaskStage.EVALUATION,
            stageArtifacts = artifacts,
            constraints = emptyList(),
            currentTask = null
        )
        result shouldContain "Plan artifact"
        result shouldContain "Exec artifact"
    }

    test("does not include artifacts from current or later stages") {
        val artifacts = mapOf(
            TaskStage.EVALUATION to "Eval artifact"
        )
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            currentTaskStage = TaskStage.EXECUTION,
            stageArtifacts = artifacts,
            constraints = emptyList(),
            currentTask = null
        )
        result shouldNotContain "Eval artifact"
    }

    test("includes current task when not null") {
        val result = useCase(
            globalPrefix = "",
            chatPrompt = "",
            currentTaskStage = TaskStage.PLANNING,
            stageArtifacts = emptyMap(),
            constraints = emptyList(),
            currentTask = "Write unit tests"
        )
        result shouldContain "Write unit tests"
    }
})
