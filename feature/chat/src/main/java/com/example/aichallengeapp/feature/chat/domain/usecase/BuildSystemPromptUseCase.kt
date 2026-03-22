package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.Constraint
import com.example.aichallengeapp.feature.chat.domain.PromptTemplates

class BuildSystemPromptUseCase {
    operator fun invoke(
        globalPrefix: String,
        chatPrompt: String,
        constraints: List<Constraint>,
        taskMemory: String? = null
    ): String {
        val parts = buildList {
            add(PromptTemplates.BASE_SYSTEM_PROMPT)
            if (globalPrefix.isNotBlank()) add(globalPrefix)
            if (!taskMemory.isNullOrBlank()) add("## Память задачи\n$taskMemory")
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
        }
        return parts.joinToString("\n\n")
    }
}
