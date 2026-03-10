package com.example.mcpserver.github

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class GitHubApiClient(private val token: String?) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun get(path: String): String {
        val response = client.get("https://api.github.com$path") {
            header("Accept", "application/vnd.github.v3+json")
            header("User-Agent", "AIChallengeApp-MCP-Server")
            if (!token.isNullOrBlank()) {
                header("Authorization", "Bearer $token")
            }
        }
        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            when (response.status.value) {
                401, 403 -> throw GitHubAuthException("GitHub auth error (${response.status}): $body")
                404 -> throw GitHubNotFoundException("GitHub resource not found: $path")
                else -> throw GitHubApiException("GitHub API error (${response.status}): $body")
            }
        }
        return response.bodyAsText()
    }
}

open class GitHubApiException(message: String) : Exception(message)
class GitHubAuthException(message: String) : GitHubApiException(message)
class GitHubNotFoundException(message: String) : GitHubApiException(message)
