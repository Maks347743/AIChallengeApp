package com.example.ragserver.config

interface ConfigRepository {
    fun load(): RagConfig
    fun save(config: RagConfig)
}
