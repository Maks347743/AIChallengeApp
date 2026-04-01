package com.example.supportmcpserver.tools

import com.example.aichallengeapp.core.mcp.model.McpCallToolResult
import com.example.aichallengeapp.core.mcp.model.McpContent
import com.example.supportmcpserver.data.SupportTicket
import com.example.supportmcpserver.data.TicketRepository
import com.example.supportmcpserver.data.UserRepository
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

class CreateTicketTool(
    private val ticketRepository: TicketRepository,
    private val userRepository: UserRepository
) : SupportToolHandler {

    override val name = "create_ticket"

    override val description =
        "Create a new support ticket for a user. Requires user identification (user_id or email), a subject, and a description of the issue. Returns the new ticket ID."

    override val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("user_id") {
                put("type", "string")
                put("description", "The user's unique ID (e.g. usr-001)")
            }
            putJsonObject("email") {
                put("type", "string")
                put("description", "The user's email address")
            }
            putJsonObject("subject") {
                put("type", "string")
                put("description", "Short title of the issue (max 100 characters)")
            }
            putJsonObject("description") {
                put("type", "string")
                put("description", "Detailed description of the problem")
            }
            putJsonObject("priority") {
                put("type", "string")
                put("description", "Ticket priority: low, medium, high (default: medium)")
            }
        }
        putJsonArray("required") {
            add(JsonPrimitive("subject"))
            add(JsonPrimitive("description"))
        }
    }

    override suspend fun execute(arguments: JsonObject?): McpCallToolResult {
        val subject = arguments?.get("subject")?.jsonPrimitive?.content
            ?: return McpCallToolResult(
                content = listOf(McpContent(text = "Missing required parameter: subject")),
                isError = true
            )
        val description = arguments["description"]?.jsonPrimitive?.content
            ?: return McpCallToolResult(
                content = listOf(McpContent(text = "Missing required parameter: description")),
                isError = true
            )

        val userId = arguments["user_id"]?.jsonPrimitive?.contentOrNull
        val email = arguments["email"]?.jsonPrimitive?.contentOrNull
        val priority = arguments["priority"]?.jsonPrimitive?.contentOrNull ?: "medium"

        val user = when {
            userId != null -> userRepository.findById(userId)
            email != null -> userRepository.findByEmail(email)
            else -> null
        }

        if (user == null && (userId != null || email != null)) {
            val lookup = userId ?: email
            return McpCallToolResult(
                content = listOf(McpContent(text = "Cannot create ticket: user not found for '$lookup'")),
                isError = true
            )
        }

        val today = LocalDate.now().toString()
        val ticket = SupportTicket(
            id = ticketRepository.nextId(),
            userId = user?.id ?: "unknown",
            subject = subject,
            description = description,
            status = "open",
            priority = priority,
            createdAt = today,
            updatedAt = today
        )

        ticketRepository.create(ticket)

        val text = buildString {
            appendLine("Ticket created successfully!")
            appendLine()
            appendLine("Ticket ID: ${ticket.id}")
            appendLine("Subject: ${ticket.subject}")
            appendLine("Priority: ${ticket.priority}")
            appendLine("Status: open")
            appendLine("Created: ${ticket.createdAt}")
            if (user != null) {
                appendLine("User: ${user.name} (${user.email})")
            }
            appendLine()
            appendLine("Our support team will respond within 24 hours.")
        }
        return McpCallToolResult(content = listOf(McpContent(text = text.trim())))
    }
}
