package com.example.ragserver

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.ragserver.chunking.StructuralChunkingStrategy
import com.example.ragserver.config.FileConfigRepository
import com.example.ragserver.data.ChunkStorage
import com.example.ragserver.data.DocumentStorage
import com.example.ragserver.data.VectorIndex
import com.example.ragserver.deepwiki.DeepWikiClient
import com.example.ragserver.deepwiki.DeepWikiImportService
import com.example.ragserver.embedding.OllamaEmbeddingService
import com.example.ragserver.mcp.RagMcpRequestHandler
import com.example.ragserver.mcp.ragMcpRoutes
import com.example.ragserver.mcp.RagToolRegistry
import com.example.ragserver.mcp.RetrievalPipeline
import com.example.ragserver.mcp.tools.RetrieveTool
import com.example.ragserver.network.sharedJson
import com.example.ragserver.query.DeepSeekQueryRewriter
import com.example.ragserver.query.OllamaQueryRewriter
import com.example.ragserver.query.QueryRewriter
import com.example.ragserver.reranking.JinaReranker
import com.example.ragserver.reranking.OllamaReranker
import com.example.ragserver.reranking.Reranker
import com.example.ragserver.service.IndexingService
import com.example.ragserver.ui.App
import com.example.ragserver.ui.SettingsState
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.walk

private const val SERVER_PORT = 3002

fun main(args: Array<String>) {
    val importIndex = args.indexOf("--import")
    when {
        importIndex >= 0 -> {
            val folderPath = args.getOrNull(importIndex + 1)
                ?: error("--import requires a folder path argument")
            startImport(folderPath)
        }
        args.contains("--headless") -> startHeadless()
        else -> startWithUi()
    }
}

private fun startImport(folderPath: String) {
    val folder = Paths.get(folderPath)
    require(folder.isDirectory()) { "Path is not a directory: $folderPath" }

    val paths = RagServerPaths.fromHome()
    RagServerPaths.initDirectories(paths)

    val configRepo = FileConfigRepository(paths.configFile, sharedJson)
    val settingsState = SettingsState(configRepo)

    val documentStorage = DocumentStorage(paths.docs)
    val chunkStorage = ChunkStorage(paths.chunks, sharedJson)
    val vectorIndex = VectorIndex()
    val embeddingService = OllamaEmbeddingService(
        baseUrlProvider = { settingsState.ollamaBaseUrl },
        modelProvider = { settingsState.ollamaEmbeddingModel }
    )
    val indexingService = IndexingService(
        documentStorage = documentStorage,
        chunkStorage = chunkStorage,
        embeddingService = embeddingService,
        vectorIndex = vectorIndex,
        indexPath = paths.indexBin
    )

    vectorIndex.load(paths.indexBin)

    val extensions = setOf("md", "txt", "rst")
    val files = folder.walk()
        .filter { it.isRegularFile() && it.name.substringAfterLast('.').lowercase() in extensions }
        .toList()

    println("Found ${files.size} files in $folderPath")

    // Deduplication: remove existing docs with the same source path
    val existingBySource = documentStorage.loadAll().associateBy { it.source }

    files.forEach { file ->
        val source = file.toAbsolutePath().toString()
        val title = file.name
        existingBySource[source]?.let { documentStorage.delete(it.id) }
        val content = file.readText()
        documentStorage.create(title = title, source = source, content = content)
        println("Imported: $title")
    }

    println("Starting indexing...")
    runBlocking {
        indexingService.runIndexing(StructuralChunkingStrategy())
        indexingService.logs.forEach { println(it) }
    }
    println("Import complete. ${indexingService.indexedChunks} chunks indexed.")
}

private fun buildMcpHandler(settingsState: SettingsState): RagMcpRequestHandler {
    val paths = RagServerPaths.fromHome()
    val chunkStorage = ChunkStorage(paths.chunks, sharedJson)
    val vectorIndex = VectorIndex()
    val embeddingService = OllamaEmbeddingService(
        baseUrlProvider = { settingsState.ollamaBaseUrl },
        modelProvider = { settingsState.ollamaEmbeddingModel }
    )

    vectorIndex.load(paths.indexBin)

    val cloudQueryRewriter = DeepSeekQueryRewriter(apiKeyProvider = { settingsState.deepSeekApiKey })
    val localQueryRewriter = OllamaQueryRewriter(
        baseUrlProvider = { settingsState.ollamaBaseUrl },
        modelProvider = { settingsState.ollamaChatModel }
    )
    val cloudReranker = JinaReranker(apiKeyProvider = { settingsState.jinaApiKey })
    val localReranker = OllamaReranker(
        baseUrlProvider = { settingsState.ollamaBaseUrl },
        modelProvider = { settingsState.ollamaChatModel }
    )

    val queryRewriter = object : QueryRewriter {
        override suspend fun rewrite(query: String) =
            if (settingsState.useLocalModel) localQueryRewriter.rewrite(query)
            else cloudQueryRewriter.rewrite(query)
    }
    val reranker = object : Reranker {
        override suspend fun rerank(query: String, documents: List<String>) =
            if (settingsState.useLocalModel) localReranker.rerank(query, documents)
            else cloudReranker.rerank(query, documents)
    }

    val pipeline = RetrievalPipeline(
        embeddingService = embeddingService,
        vectorIndex = vectorIndex,
        chunkStorage = chunkStorage,
        queryRewriter = queryRewriter,
        reranker = reranker,
        configProvider = { settingsState.toConfig() }
    )

    val retrieveTool = RetrieveTool(pipeline, configProvider = { settingsState.toConfig() })
    val toolRegistry = RagToolRegistry(retrieveTool)
    return RagMcpRequestHandler(toolRegistry)
}

private fun startHeadless() {
    val paths = RagServerPaths.fromHome()
    RagServerPaths.initDirectories(paths)
    val configRepo = FileConfigRepository(paths.configFile, sharedJson)
    val settingsState = SettingsState(configRepo)
    val mcpHandler = buildMcpHandler(settingsState)

    println("Starting RAG Server (headless) on port $SERVER_PORT")
    runBlocking {
        embeddedServer(Netty, port = SERVER_PORT) {
            install(ContentNegotiation) { json(sharedJson) }
            routing { ragMcpRoutes(mcpHandler) }
        }.start(wait = true)
    }
}

private fun startWithUi() = application {
    val paths = RagServerPaths.fromHome()
    RagServerPaths.initDirectories(paths)

    val configRepo = FileConfigRepository(paths.configFile, sharedJson)
    val settingsState = SettingsState(configRepo)

    val documentStorage = DocumentStorage(paths.docs)
    val chunkStorage = ChunkStorage(paths.chunks, sharedJson)
    val vectorIndex = VectorIndex()
    val embeddingService = OllamaEmbeddingService(
        baseUrlProvider = { settingsState.ollamaBaseUrl },
        modelProvider = { settingsState.ollamaEmbeddingModel }
    )

    val indexingService = IndexingService(
        documentStorage = documentStorage,
        chunkStorage = chunkStorage,
        embeddingService = embeddingService,
        vectorIndex = vectorIndex,
        indexPath = paths.indexBin
    )

    // Attempt to load a previously saved index on startup
    vectorIndex.load(paths.indexBin)

    val deepWikiImportService = DeepWikiImportService(DeepWikiClient(), documentStorage)
    val mcpHandler = buildMcpHandler(settingsState)

    val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    serverScope.launch {
        embeddedServer(Netty, port = SERVER_PORT) {
            install(ContentNegotiation) { json(sharedJson) }
            routing { ragMcpRoutes(mcpHandler) }
        }.start(wait = false)
    }

    Window(onCloseRequest = ::exitApplication, title = "RAG Server — Port $SERVER_PORT") {
        App(indexingService, documentStorage, deepWikiImportService, settingsState)
    }
}
