package com.example.filesystemmcpserver

import com.example.filesystemmcpserver.mcp.FilesystemMcpRequestHandler
import com.example.filesystemmcpserver.mcp.FilesystemToolRegistry
import com.example.filesystemmcpserver.routes.filesystemMcpRoutes
import com.example.filesystemmcpserver.tools.CreateFileTool
import com.example.filesystemmcpserver.tools.ListFilesTool
import com.example.filesystemmcpserver.tools.ReadFileTool
import com.example.filesystemmcpserver.tools.SearchInFilesTool
import com.example.filesystemmcpserver.tools.WriteFileTool
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    val projectDir = args.indexOf("--project-dir")
        .takeIf { it >= 0 }
        ?.let { args.getOrNull(it + 1) }
        ?: System.getProperty("user.dir")

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    val registry = FilesystemToolRegistry(
        listOf(
            ReadFileTool(projectDir),
            ListFilesTool(projectDir),
            SearchInFilesTool(projectDir),
            WriteFileTool(projectDir),
            CreateFileTool(projectDir)
        )
    )

    val handler = FilesystemMcpRequestHandler(registry)

    embeddedServer(Netty, port = 3004) {
        install(ContentNegotiation) {
            json(json)
        }
        routing {
            filesystemMcpRoutes(handler)
        }
    }.start(wait = true)
}
