package com.example.mcpserver.github

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
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
        val questionIdx = path.indexOf('?')
        val basePath = if (questionIdx >= 0) path.substring(0, questionIdx) else path
        val queryParams = if (questionIdx >= 0) parseQueryParams(path.substring(questionIdx + 1)) else emptyList()

        val response = client.get("https://api.github.com$basePath") {
            header("Accept", "application/vnd.github.v3+json")
            header("User-Agent", "AIChallengeApp-MCP-Server")
            if (!token.isNullOrBlank()) {
                header("Authorization", "Bearer $token")
            }
            queryParams.forEach { (key, value) -> parameter(key, value) }
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

    private fun parseQueryParams(query: String): List<Pair<String, String>> {
        return query.split("&").mapNotNull { param ->
            val eqIdx = param.indexOf('=')
            if (eqIdx > 0) param.substring(0, eqIdx) to param.substring(eqIdx + 1)
            else null
        }
    }
}

open class GitHubApiException(message: String) : Exception(message)
class GitHubAuthException(message: String) : GitHubApiException(message)
class GitHubNotFoundException(message: String) : GitHubApiException(message)
