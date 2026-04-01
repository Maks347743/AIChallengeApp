package com.example.supportmcpserver.tools

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import com.example.supportmcpserver.data.FaqRepository
import com.example.supportmcpserver.mcp.SupportToolHandler
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull

class SearchFaqTool(private val faqRepository: FaqRepository) : SupportToolHandler {

    override val name = "search_faq"

    override val description =
        "Search the customer support FAQ knowledge base by keywords. Returns matching FAQ entries with questions and answers. Always call this first when a user asks a product question."

    override val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("query") {
                put("type", "string")
                put("description", "Search keywords describing the user's issue (e.g. 'password reset', 'billing charge', '2fa not working')")
            }
            putJsonObject("max_results") {
                put("type", "integer")
                put("description", "Maximum number of FAQ entries to return (default: 3)")
            }
        }
        putJsonArray("required") { add(JsonPrimitive("query")) }
    }

    override suspend fun execute(arguments: JsonObject?): McpCallToolResult {
        val query = arguments?.get("query")?.jsonPrimitive?.content
            ?: return McpCallToolResult(
                content = listOf(McpContent(text = "Missing required parameter: query")),
                isError = true
            )
        val maxResults = arguments["max_results"]?.jsonPrimitive?.intOrNull ?: 3

        val results = faqRepository.search(query, maxResults)

        if (results.isEmpty()) {
            return McpCallToolResult(
                content = listOf(McpContent(text = "No FAQ entries found for: \"$query\". Consider escalating to a human agent."))
            )
        }

        val text = buildString {
            appendLine("Found ${results.size} FAQ entry(ies) for \"$query\":\n")
            results.forEachIndexed { index, entry ->
                appendLine("${index + 1}. Q: ${entry.question}")
                appendLine("   A: ${entry.answer}")
                appendLine()
            }
        }
        return McpCallToolResult(content = listOf(McpContent(text = text.trim())))
    }
}
