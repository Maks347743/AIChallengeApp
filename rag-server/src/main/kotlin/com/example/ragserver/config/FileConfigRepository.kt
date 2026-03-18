package com.example.ragserver.config

import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class FileConfigRepository(
    private val configPath: Path,
    private val json: Json
) : ConfigRepository {

    override fun load(): RagConfig {
        if (!configPath.exists()) return RagConfig()
        return runCatching {
            json.decodeFromString<RagConfig>(configPath.readText())
        }.getOrDefault(RagConfig())
    }

    override fun save(config: RagConfig) {
        runCatching {
            configPath.parent?.toFile()?.mkdirs()
            configPath.writeText(json.encodeToString(RagConfig.serializer(), config))
        }
    }
}
