package com.example.supportmcpserver.data

import java.sql.Connection
import java.sql.ResultSet

data class SupportTicket(
    val id: String,
    val userId: String,
    val subject: String,
    val description: String,
    val status: String,
    val priority: String,
    val createdAt: String,
    val updatedAt: String,
    val resolution: String? = null
)

class TicketRepository(private val connection: Connection) {

    fun findByUserId(userId: String): List<SupportTicket> =
        connection.prepareStatement("SELECT * FROM tickets WHERE user_id = ? COLLATE NOCASE").use { ps ->
            ps.setString(1, userId)
            ps.executeQuery().toList()
        }

    fun findById(ticketId: String): SupportTicket? =
        connection.prepareStatement("SELECT * FROM tickets WHERE id = ? COLLATE NOCASE").use { ps ->
            ps.setString(1, ticketId)
            ps.executeQuery().takeIf { it.next() }?.toTicket()
        }

    fun create(ticket: SupportTicket): SupportTicket {
        connection.prepareStatement(
            "INSERT INTO tickets (id, user_id, subject, description, status, priority, created_at, updated_at, resolution) VALUES (?,?,?,?,?,?,?,?,?)"
        ).use { ps ->
            ps.setString(1, ticket.id)
            ps.setString(2, ticket.userId)
            ps.setString(3, ticket.subject)
            ps.setString(4, ticket.description)
            ps.setString(5, ticket.status)
            ps.setString(6, ticket.priority)
            ps.setString(7, ticket.createdAt)
            ps.setString(8, ticket.updatedAt)
            ps.setString(9, ticket.resolution)
            ps.executeUpdate()
        }
        return ticket
    }

    fun update(updated: SupportTicket): Boolean {
        val rows = connection.prepareStatement(
            "UPDATE tickets SET user_id=?, subject=?, description=?, status=?, priority=?, created_at=?, updated_at=?, resolution=? WHERE id=? COLLATE NOCASE"
        ).use { ps ->
            ps.setString(1, updated.userId)
            ps.setString(2, updated.subject)
            ps.setString(3, updated.description)
            ps.setString(4, updated.status)
            ps.setString(5, updated.priority)
            ps.setString(6, updated.createdAt)
            ps.setString(7, updated.updatedAt)
            ps.setString(8, updated.resolution)
            ps.setString(9, updated.id)
            ps.executeUpdate()
        }
        return rows > 0
    }

    fun nextId(): String {
        val count = connection.prepareStatement("SELECT COUNT(*) FROM tickets").use {
            it.executeQuery().getInt(1)
        }
        return "tkt-%03d".format(count + 1)
    }

    private fun ResultSet.toList(): List<SupportTicket> {
        val result = mutableListOf<SupportTicket>()
        while (next()) result.add(toTicket())
        return result
    }

    private fun ResultSet.toTicket() = SupportTicket(
        id          = getString("id"),
        userId      = getString("user_id"),
        subject     = getString("subject"),
        description = getString("description"),
        status      = getString("status"),
        priority    = getString("priority"),
        createdAt   = getString("created_at"),
        updatedAt   = getString("updated_at"),
        resolution  = getString("resolution")
    )
}
