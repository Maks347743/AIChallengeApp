package com.example.ragserver.embedding

import com.example.ragserver.ApiEndpoints
import com.example.ragserver.network.TIMEOUT_EMBEDDING_MS
import com.example.ragserver.network.createHttpClient
import com.example.ragserver.network.sharedJson
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OllamaEmbeddingService(
    private val baseUrlProvider: () -> String = { ApiEndpoints.OLLAMA_DEFAULT_BASE_URL },
    private val modelProvider: () -> String = { ApiEndpoints.OLLAMA_DEFAULT_EMBEDDING_MODEL }
) : EmbeddingService {
    private val client = createHttpClient(timeoutMs = TIMEOUT_EMBEDDING_MS)

    @Serializable
    private data class EmbedRequest(val model: String, val prompt: String)

    /**
     * Returns null if Ollama returns an error or unexpected response.
     * Caller should skip null embeddings.
     */
    override suspend fun embed(text: String): FloatArray? {
        if (text.isBlank()) return null

        val rawBody = try {
            client.post("${baseUrlProvider()}/api/embeddings") {
                contentType(ContentType.Application.Json)
                setBody(EmbedRequest(modelProvider(), text.take(EMBED_TEXT_CHAR_LIMIT)))
            }.bodyAsText()
        } catch (e: Exception) {
            throw Exception("Ollama request failed: ${e.message}")
        }

        return try {
            val root = sharedJson.parseToJsonElement(rawBody).jsonObject

            // /api/embeddings → { "embedding": [...] }
            root["embedding"]?.jsonArray
                ?.map { it.jsonPrimitive.content.toFloat() }
                ?.toFloatArray()
            // if null, fall through to error
                ?: run {
                    val error = root["error"]?.jsonPrimitive?.content
                    throw Exception("Ollama error: ${error ?: "unexpected response: $rawBody"}")
                }
        } catch (e: Exception) {
            throw Exception("Failed to parse Ollama response: ${e.message}\nRaw: ${rawBody.take(ERROR_PREVIEW_CHARS)}")
        }
    }

    private companion object {
        // nomic-embed-text has an 8192 token limit; ~3000 chars is a safe character budget
        const val EMBED_TEXT_CHAR_LIMIT = 3000
        const val ERROR_PREVIEW_CHARS = 300
    }
}
