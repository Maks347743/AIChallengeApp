package com.example.ragserver.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ragserver.config.ConfigRepository
import com.example.ragserver.config.RagConfig

class SettingsState(private val repo: ConfigRepository) {
    private val loaded = repo.load()

    var useQueryRewrite by mutableStateOf(loaded.useQueryRewrite)
    var deepSeekApiKey by mutableStateOf(loaded.deepSeekApiKey)
    var useRerank by mutableStateOf(loaded.useRerank)
    var jinaApiKey by mutableStateOf(loaded.jinaApiKey)
    var topK by mutableStateOf(loaded.topK)
    var initialK by mutableStateOf(loaded.initialK)
    var similarityThreshold by mutableStateOf(loaded.similarityThreshold)

    fun toConfig() = RagConfig(
        useQueryRewrite = useQueryRewrite,
        deepSeekApiKey = deepSeekApiKey,
        useRerank = useRerank,
        jinaApiKey = jinaApiKey,
        topK = topK,
        initialK = initialK,
        similarityThreshold = similarityThreshold
    )

    fun save() {
        repo.save(toConfig())
    }
}
