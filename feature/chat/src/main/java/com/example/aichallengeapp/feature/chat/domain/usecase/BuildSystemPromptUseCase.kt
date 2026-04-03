package com.example.aichallengeapp.feature.chat.domain.usecase

import com.example.aichallengeapp.core.database.domain.model.Constraint
import com.example.aichallengeapp.feature.chat.domain.PromptTemplates

class BuildSystemPromptUseCase {
    operator fun invoke(
        globalPrefix: String,
        chatPrompt: String,
        constraints: List<Constraint>,
        taskMemory: String? = null,
        supportEnabled: Boolean = true,
        maxLength: Int? = null
    ): String {
        val parts = buildList {
            if (supportEnabled) add(PromptTemplates.SUPPORT_SYSTEM_PROMPT)
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
        val result = parts.joinToString("\n\n")
        return if (maxLength != null && result.length > maxLength) result.take(maxLength) else result
    }
}
