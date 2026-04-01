package com.example.supportmcpserver.tools

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import com.example.supportmcpserver.data.TicketRepository
import com.example.supportmcpserver.mcp.SupportToolHandler
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.jsonPrimitive

class GetUserTicketsTool(private val ticketRepository: TicketRepository) : SupportToolHandler {

    override val name = "get_user_tickets"

    override val description =
        "Get all support tickets for a user. Returns a summary list with ticket IDs, subjects, statuses, and priorities. Use get_ticket_details to get the full details of a specific ticket."

    override val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("user_id") {
                put("type", "string")
                put("description", "The user's unique ID (e.g. usr-001)")
            }
        }
        putJsonArray("required") { add(JsonPrimitive("user_id")) }
    }

    override suspend fun execute(arguments: JsonObject?): McpCallToolResult {
        val userId = arguments?.get("user_id")?.jsonPrimitive?.content
            ?: return McpCallToolResult(
                content = listOf(McpContent(text = "Missing required parameter: user_id")),
                isError = true
            )

        val tickets = ticketRepository.findByUserId(userId)

        if (tickets.isEmpty()) {
            return McpCallToolResult(
                content = listOf(McpContent(text = "No tickets found for user: $userId"))
            )
        }

        val text = buildString {
            appendLine("${tickets.size} ticket(s) for user $userId:\n")
            tickets.forEach { t ->
                appendLine("[${t.id}] ${t.subject}")
                appendLine("  Status: ${t.status} | Priority: ${t.priority} | Updated: ${t.updatedAt}")
            }
        }
        return McpCallToolResult(content = listOf(McpContent(text = text.trim())))
    }
}
