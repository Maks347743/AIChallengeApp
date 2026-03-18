package com.example.ragserver

import java.nio.file.Path
import java.nio.file.Paths

data class RagServerPaths(
    val base: Path,
    val docs: Path,
    val index: Path,
    val chunks: Path,
    val configFile: Path,
    val indexBin: Path
) {
    companion object {
        fun fromHome(): RagServerPaths {
            val base = Paths.get(System.getProperty("user.home"), ".ragserver")
            return RagServerPaths(
                base = base,
                docs = base.resolve("docs"),
                index = base.resolve("index"),
                chunks = base.resolve("chunks"),
                configFile = base.resolve("config.json"),
                indexBin = base.resolve("index/index.bin")
            )
        }

        fun initDirectories(paths: RagServerPaths) {
            paths.docs.toFile().mkdirs()
            paths.index.toFile().mkdirs()
            paths.chunks.toFile().mkdirs()
        }
    }
}
