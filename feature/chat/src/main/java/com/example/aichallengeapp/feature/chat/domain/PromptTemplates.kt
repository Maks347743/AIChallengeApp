package com.example.aichallengeapp.feature.chat.domain

object PromptTemplates {
    const val ROLE_LABEL_ASSISTANT = "Ассистент"
    const val ROLE_LABEL_USER = "Пользователь"

    const val SUMMARIZER_SYSTEM_PROMPT = "Ты краткий суммаризатор переписок."
    const val FACTS_EXTRACTOR_SYSTEM_PROMPT =
        "Ты краткий суммаризатор переписок, который может вычленять важные данные из переписки"

    const val BASE_SYSTEM_PROMPT =
        "Ты — полезный AI-ассистент. Помогай пользователю выполнять задачи конкретно и последовательно.\n" +
        "Если тебе доступны инструменты (tools) — активно используй их для выполнения задач пользователя. " +
        "Когда задача требует нескольких последовательных вызовов инструментов, используй run_pipeline " +
        "для объединения шагов в цепочку."
}
