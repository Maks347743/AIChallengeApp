package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.Constraint
import com.example.aichallengeapp.core.database.domain.model.TaskStage
import com.example.aichallengeapp.feature.chat.domain.PromptTemplates

class BuildSystemPromptUseCase {
    operator fun invoke(
        globalPrefix: String,
        chatPrompt: String,
        currentTaskStage: TaskStage,
        stageArtifacts: Map<TaskStage, String>,
        constraints: List<Constraint>,
        currentTask: String?
    ): String {
        val allStages = TaskStage.entries
        val currentIndex = allStages.indexOf(currentTaskStage)
        val precedingStages = allStages.take(currentIndex)
        val relevantArtifacts = precedingStages.mapNotNull { stage ->
            stageArtifacts[stage]?.let { stage to it }
        }
        val parts = buildList {
            if (globalPrefix.isNotBlank()) add(globalPrefix)
            if (chatPrompt.isNotBlank()) add(chatPrompt)
            if (constraints.isNotEmpty()) {
                val block = buildString {
                    append("ВАЖНО: Следующие ограничения ЗАПРЕЩЕНО нарушать:")
                    constraints.forEachIndexed { i, c ->
                        append("\n${i + 1}. ${c.name}: ${c.description}")
                    }
                }
                add(block)
            }
            add(PromptTemplates.stagePrompt(currentTaskStage))
            if (relevantArtifacts.isNotEmpty()) {
                val artifactsSection = buildString {
                    append("Артефакты предыдущих этапов:")
                    relevantArtifacts.forEach { (stage, artifact) ->
                        val stageLabel = when (stage) {
                            TaskStage.PLANNING -> "Планирование"
                            TaskStage.EXECUTION -> "Выполнение"
                            TaskStage.EVALUATION -> "Оценка"
                            TaskStage.DONE -> "Завершено"
                        }
                        append("\n$stageLabel:\n$artifact")
                    }
                }
                add(artifactsSection)
            }
            if (!currentTask.isNullOrBlank()) add("Текущая задача:\n$currentTask")
        }
        return parts.joinToString("\n\n")
    }
}
