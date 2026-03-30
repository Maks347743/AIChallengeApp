package com.example.githubmcpserver

import com.example.githubmcpserver.github.GitHubApiClient
import com.example.githubmcpserver.mcp.GitHubMcpRequestHandler
import com.example.githubmcpserver.mcp.GitHubToolRegistry
import com.example.githubmcpserver.routes.gitHubMcpRoutes
import com.example.githubmcpserver.tools.GitBranchTool
import com.example.githubmcpserver.tools.GitDiffTool
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

fun main(args: Array<String>) {
    val githubToken = System.getenv("GITHUB_TOKEN")
    val gitHubClient = GitHubApiClient(githubToken)

    val projectDir = args.indexOf("--project-dir")
        .takeIf { it >= 0 }
        ?.let { args.getOrNull(it + 1) }
        ?: System.getProperty("user.dir")

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    val registry = GitHubToolRegistry(
        listOf(
            GitHubSearchReposTool(gitHubClient, json),
            GitHubGetUserTool(gitHubClient, json),
            GitHubTrendingTool(gitHubClient, json),
            GitBranchTool(projectDir),
            GitDiffTool(projectDir)
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
