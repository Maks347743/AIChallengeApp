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

class GetTicketDetailsTool(private val ticketRepository: TicketRepository) : SupportToolHandler {

    override val name = "get_ticket_details"

    override val description =
        "Get the full details of a specific support ticket, including description and resolution. Use this after get_user_tickets to inspect a particular ticket."

    override val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("ticket_id") {
                put("type", "string")
                put("description", "The ticket ID (e.g. tkt-001)")
            }
        }
        putJsonArray("required") { add(JsonPrimitive("ticket_id")) }
    }

    override suspend fun execute(arguments: JsonObject?): McpCallToolResult {
        val ticketId = arguments?.get("ticket_id")?.jsonPrimitive?.content
            ?: return McpCallToolResult(
                content = listOf(McpContent(text = "Missing required parameter: ticket_id")),
                isError = true
            )

        val ticket = ticketRepository.findById(ticketId)
            ?: return McpCallToolResult(
                content = listOf(McpContent(text = "No ticket found with ID: $ticketId"))
            )

        val text = buildString {
            appendLine("Ticket: ${ticket.id}")
            appendLine("Subject: ${ticket.subject}")
            appendLine("Status: ${ticket.status} | Priority: ${ticket.priority}")
            appendLine("Created: ${ticket.createdAt} | Updated: ${ticket.updatedAt}")
            appendLine()
            appendLine("Description:")
            appendLine(ticket.description)
            if (ticket.resolution != null) {
                appendLine()
                appendLine("Resolution:")
                appendLine(ticket.resolution)
            }
        }
        return McpCallToolResult(content = listOf(McpContent(text = text.trim())))
    }
}
