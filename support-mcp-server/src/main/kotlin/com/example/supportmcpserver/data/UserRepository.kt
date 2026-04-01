package com.example.supportmcpserver.data

import java.sql.Connection
import java.sql.ResultSet

data class CrmUser(
    val id: String,
    val name: String,
    val email: String,
    val plan: String,
    val status: String,
    val createdAt: String,
    val location: String
)

class UserRepository(private val connection: Connection) {

    fun findById(userId: String): CrmUser? =
        connection.prepareStatement("SELECT * FROM users WHERE id = ? COLLATE NOCASE").use { ps ->
            ps.setString(1, userId)
            ps.executeQuery().takeIf { it.next() }?.toCrmUser()
        }

    fun findByEmail(email: String): CrmUser? =
        connection.prepareStatement("SELECT * FROM users WHERE email = ? COLLATE NOCASE").use { ps ->
            ps.setString(1, email)
            ps.executeQuery().takeIf { it.next() }?.toCrmUser()
        }

    private fun ResultSet.toCrmUser() = CrmUser(
        id        = getString("id"),
        name      = getString("name"),
        email     = getString("email"),
        plan      = getString("plan"),
        status    = getString("status"),
        createdAt = getString("created_at"),
        location  = getString("location")
    )
}
