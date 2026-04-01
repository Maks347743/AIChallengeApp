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
import kotlinx.serialization.json.contentOrNull
import java.time.LocalDate

class CloseTicketTool(private val ticketRepository: TicketRepository) : SupportToolHandler {

    override val name = "close_ticket"

    override val description =
        "Close a support ticket and mark it as resolved. Use this when the user confirms their issue is resolved. Optionally include a resolution summary."

    override val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("ticket_id") {
                put("type", "string")
                put("description", "The ticket ID to close (e.g. tkt-001)")
            }
            putJsonObject("resolution") {
                put("type", "string")
                put("description", "Brief summary of how the issue was resolved")
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
        val resolution = arguments["resolution"]?.jsonPrimitive?.contentOrNull
            ?: "Issue resolved by user."

        val ticket = ticketRepository.findById(ticketId)
            ?: return McpCallToolResult(
                content = listOf(McpContent(text = "No ticket found with ID: $ticketId")),
                isError = true
            )

        if (ticket.status == "resolved") {
            return McpCallToolResult(
                content = listOf(McpContent(text = "Ticket ${ticket.id} is already resolved."))
            )
        }

        val closed = ticket.copy(
            status = "resolved",
            updatedAt = LocalDate.now().toString(),
            resolution = resolution
        )
        ticketRepository.update(closed)

        return McpCallToolResult(
            content = listOf(McpContent(text = "Ticket ${ticket.id} closed.\nSubject: ${ticket.subject}\nResolution: $resolution"))
        )
    }
}
