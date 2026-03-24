package com.example.aichallengeapp.feature.settings.domain.model

data class ModelEndpoint(
    val modelId: String,
    val baseUrlOverride: String?,
    val apiKeyOverride: String?
)

fun ChatSettings.resolveEndpoint(): ModelEndpoint =
    if (model == DeepSeekModel.OLLAMA) ModelEndpoint(ollamaModelName, ollamaBaseUrl, "")
    else ModelEndpoint(model.id, null, null)
