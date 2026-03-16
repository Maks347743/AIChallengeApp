package com.example.ragserver

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.aichallengeapp.core.mcp.model.JsonRpcRequest
import com.example.ragserver.data.ChunkStorage
import com.example.ragserver.data.DocumentStorage
import com.example.ragserver.data.VectorIndex
import com.example.ragserver.embedding.OllamaEmbeddingService
import com.example.ragserver.mcp.RagMcpRequestHandler
import com.example.ragserver.mcp.RagToolRegistry
import com.example.ragserver.mcp.tools.RetrieveTool
import com.example.ragserver.deepwiki.DeepWikiClient
import com.example.ragserver.deepwiki.DeepWikiImportService
import com.example.ragserver.service.IndexingService
import com.example.ragserver.ui.App
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.nio.file.Paths
import java.util.UUID

fun main() = application {
    val basePath = Paths.get(System.getProperty("user.home"), ".ragserver")
    val docsPath = basePath.resolve("docs")
    val indexPath = basePath.resolve("index")
    val chunksPath = basePath.resolve("chunks")

    docsPath.toFile().mkdirs()
    indexPath.toFile().mkdirs()
    chunksPath.toFile().mkdirs()

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    val documentStorage = DocumentStorage(docsPath)
    val chunkStorage = ChunkStorage(chunksPath, json)
    val vectorIndex = VectorIndex()
    val embeddingService = OllamaEmbeddingService()
    val indexingService = IndexingService(
        documentStorage = documentStorage,
        chunkStorage = chunkStorage,
        embeddingService = embeddingService,
        vectorIndex = vectorIndex,
        indexPath = indexPath.resolve("index.bin")
    )

    // Attempt to load a previously saved index on startup
    vectorIndex.load(indexPath.resolve("index.bin"))

    val deepWikiImportService = DeepWikiImportService(DeepWikiClient(), documentStorage)

    val retrieveTool = RetrieveTool(embeddingService, vectorIndex, chunkStorage)
    val toolRegistry = RagToolRegistry(retrieveTool)
    val mcpHandler = RagMcpRequestHandler(toolRegistry)

    CoroutineScope(Dispatchers.IO).launch {
        embeddedServer(Netty, port = 3002) {
            install(ContentNegotiation) { json(json) }
            routing {
                ragMcpRoutes(mcpHandler)
            }
        }.start(wait = false)
    }

    Window(onCloseRequest = ::exitApplication, title = "RAG Server — Port 3002") {
        App(indexingService, documentStorage, deepWikiImportService)
    }
}

private fun Routing.ragMcpRoutes(handler: RagMcpRequestHandler) {
    post("/mcp") {
        val request = call.receive<JsonRpcRequest>()

        if (request.method == "notifications/initialized") {
            call.respond(HttpStatusCode.Accepted, "")
            return@post
        }

        val response = handler.handle(request)

        if (request.method == "initialize") {
            call.response.headers.append("Mcp-Session-Id", UUID.randomUUID().toString())
        }

        call.respond(response)
    }
}
