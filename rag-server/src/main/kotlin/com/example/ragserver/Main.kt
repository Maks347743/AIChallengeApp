package com.example.ragserver

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
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
import com.example.ragserver.query.QueryRewriter
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

fun main() = application {
    val paths = RagServerPaths.fromHome()
    RagServerPaths.initDirectories(paths)

    val configRepo = FileConfigRepository(paths.configFile, sharedJson)
    val settingsState = SettingsState(configRepo)

    val documentStorage = DocumentStorage(paths.docs)
    val chunkStorage = ChunkStorage(paths.chunks, sharedJson)
    val vectorIndex = VectorIndex()
    val embeddingService = OllamaEmbeddingService()

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

    val queryRewriter = QueryRewriter(apiKeyProvider = { settingsState.deepSeekApiKey })
    val reranker = Reranker(apiKeyProvider = { settingsState.jinaApiKey })

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
    val mcpHandler = RagMcpRequestHandler(toolRegistry)

    val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    serverScope.launch {
        embeddedServer(Netty, port = 3002) {
            install(ContentNegotiation) { json(sharedJson) }
            routing {
                ragMcpRoutes(mcpHandler)
            }
        }.start(wait = false)
    }

    Window(onCloseRequest = ::exitApplication, title = "RAG Server — Port 3002") {
        App(indexingService, documentStorage, deepWikiImportService, settingsState)
    }
}
