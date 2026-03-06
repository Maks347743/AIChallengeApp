package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.TaskStage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DetectStageTransitionUseCaseTest : FunSpec({

    test("PLANNING -> EXECUTION is allowed") {
        DetectStageTransitionUseCase.isTransitionAllowed(TaskStage.PLANNING, TaskStage.EXECUTION) shouldBe true
    }

    test("PLANNING -> EVALUATION is not allowed") {
        DetectStageTransitionUseCase.isTransitionAllowed(TaskStage.PLANNING, TaskStage.EVALUATION) shouldBe false
    }

    test("PLANNING -> DONE is not allowed") {
        DetectStageTransitionUseCase.isTransitionAllowed(TaskStage.PLANNING, TaskStage.DONE) shouldBe false
    }

    test("EXECUTION -> EVALUATION is allowed") {
        DetectStageTransitionUseCase.isTransitionAllowed(TaskStage.EXECUTION, TaskStage.EVALUATION) shouldBe true
    }

    test("EXECUTION -> PLANNING is allowed") {
        DetectStageTransitionUseCase.isTransitionAllowed(TaskStage.EXECUTION, TaskStage.PLANNING) shouldBe true
    }

    test("EXECUTION -> DONE is not allowed") {
        DetectStageTransitionUseCase.isTransitionAllowed(TaskStage.EXECUTION, TaskStage.DONE) shouldBe false
    }

    test("EVALUATION -> DONE is allowed") {
        DetectStageTransitionUseCase.isTransitionAllowed(TaskStage.EVALUATION, TaskStage.DONE) shouldBe true
    }

    test("EVALUATION -> EXECUTION is allowed") {
        DetectStageTransitionUseCase.isTransitionAllowed(TaskStage.EVALUATION, TaskStage.EXECUTION) shouldBe true
    }

    test("EVALUATION -> PLANNING is allowed") {
        DetectStageTransitionUseCase.isTransitionAllowed(TaskStage.EVALUATION, TaskStage.PLANNING) shouldBe true
    }

    test("DONE -> PLANNING is allowed") {
        DetectStageTransitionUseCase.isTransitionAllowed(TaskStage.DONE, TaskStage.PLANNING) shouldBe true
    }

    test("DONE -> EXECUTION is not allowed") {
        DetectStageTransitionUseCase.isTransitionAllowed(TaskStage.DONE, TaskStage.EXECUTION) shouldBe false
    }

    test("DONE -> EVALUATION is not allowed") {
        DetectStageTransitionUseCase.isTransitionAllowed(TaskStage.DONE, TaskStage.EVALUATION) shouldBe false
    }
})
