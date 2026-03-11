package com.example.mcpserver

import com.example.mcpserver.github.GitHubApiClient
import com.example.mcpserver.mcp.McpRequestHandler
import com.example.mcpserver.mcp.ToolRegistry
import com.example.mcpserver.routes.mcpRoutes
import com.example.mcpserver.tools.GitHubGetUserTool
import com.example.mcpserver.tools.GitHubSearchReposTool
import com.example.mcpserver.tools.GitHubTrendingTool
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun main() {
    val githubToken = System.getenv("GITHUB_TOKEN")
    val gitHubClient = GitHubApiClient(githubToken)

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    val registry = ToolRegistry()
    GitHubSearchReposTool(gitHubClient, json).register(registry)
    GitHubGetUserTool(gitHubClient, json).register(registry)
    GitHubTrendingTool(gitHubClient, json).register(registry)

    val handler = McpRequestHandler(registry)

    embeddedServer(Netty, port = 3001) {
        install(ContentNegotiation) {
            json(json)
        }
        routing {
            mcpRoutes(handler)
        }
    }.start(wait = true)
}
