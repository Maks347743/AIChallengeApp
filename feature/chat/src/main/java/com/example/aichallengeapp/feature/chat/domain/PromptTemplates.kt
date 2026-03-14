package com.example.aichallengeapp.feature.chat.domain

object PromptTemplates {
    const val ROLE_LABEL_ASSISTANT = "Ассистент"
    const val ROLE_LABEL_USER = "Пользователь"

    const val SUMMARIZER_SYSTEM_PROMPT = "Ты краткий суммаризатор переписок."
    const val FACTS_EXTRACTOR_SYSTEM_PROMPT =
        "Ты краткий суммаризатор переписок, который может вычленять важные данные из переписки"

    const val BASE_SYSTEM_PROMPT =
        "Ты — полезный AI-ассистент. Помогай пользователю выполнять задачи конкретно и последовательно.\n" +
        "Если тебе доступны инструменты (tools) — активно используй их для выполнения задач пользователя.\n\n" +
        "ВАЖНО: Когда задача требует нескольких последовательных шагов (например, 'найди X и расскажи о нём'), " +
        "ОБЯЗАТЕЛЬНО используй run_pipeline для объединения шагов в цепочку.\n" +
        "Примеры задач для run_pipeline:\n" +
        "- 'Найди популярный репозиторий и расскажи о нём' → pipeline: github_search_repos → extract → ask_question → summarize\n" +
        "- 'Найди пользователя X и покажи его репозитории' → pipeline: github_get_user → extract → github_search_repos → summarize\n" +
        "- Любая задача где результат одного инструмента нужен как вход для другого\n\n" +
        "НЕ пытайся выполнить такие задачи без инструментов — используй доступные tools."
}
