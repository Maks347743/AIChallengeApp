package com.example.githubmcpserver

import com.example.githubmcpserver.github.GitHubApiClient
import com.example.githubmcpserver.mcp.GitHubMcpRequestHandler
import com.example.githubmcpserver.mcp.GitHubToolRegistry
import com.example.githubmcpserver.routes.gitHubMcpRoutes
import com.example.githubmcpserver.tools.GitHubGetUserTool
import com.example.githubmcpserver.tools.GitHubSearchReposTool
import com.example.githubmcpserver.tools.GitHubTrendingTool
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

    val registry = GitHubToolRegistry(
        listOf(
            GitHubSearchReposTool(gitHubClient, json),
            GitHubGetUserTool(gitHubClient, json),
            GitHubTrendingTool(gitHubClient, json)
        )
    )

    val handler = GitHubMcpRequestHandler(registry)

    embeddedServer(Netty, port = 3001) {
        install(ContentNegotiation) {
            json(json)
        }
        routing {
            gitHubMcpRoutes(handler)
        }
    }.start(wait = true)
}
