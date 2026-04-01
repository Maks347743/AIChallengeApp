package com.example.supportmcpserver

import com.example.supportmcpserver.data.AppDatabase
import com.example.supportmcpserver.data.FaqRepository
import com.example.supportmcpserver.data.TicketRepository
import com.example.supportmcpserver.data.UserRepository
import com.example.supportmcpserver.mcp.SupportMcpRequestHandler
import com.example.supportmcpserver.mcp.SupportToolRegistry
import com.example.supportmcpserver.mcp.supportMcpRoutes
import com.example.supportmcpserver.tools.CloseTicketTool
import com.example.supportmcpserver.tools.CreateTicketTool
import com.example.supportmcpserver.tools.GetTicketDetailsTool
import com.example.supportmcpserver.tools.GetUserInfoTool
import com.example.supportmcpserver.tools.GetUserTicketsTool
import com.example.supportmcpserver.tools.SearchFaqTool
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun main() {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    val jarDir = File(object {}.javaClass.protectionDomain.codeSource.location.toURI()).parentFile
    val dataDir = File(jarDir, "data").also { it.mkdirs() }

    val db = AppDatabase(dataDir)

    val faqRepository = FaqRepository(db.connection)
    val userRepository = UserRepository(db.connection)
    val ticketRepository = TicketRepository(db.connection)

    val registry = SupportToolRegistry(
        listOf(
            SearchFaqTool(faqRepository),
            GetUserInfoTool(userRepository),
            GetUserTicketsTool(ticketRepository),
            GetTicketDetailsTool(ticketRepository),
            CreateTicketTool(ticketRepository, userRepository),
            CloseTicketTool(ticketRepository)
        )
    )

    val handler = SupportMcpRequestHandler(registry)

    embeddedServer(Netty, port = 3003) {
        install(ContentNegotiation) { json(json) }
        routing { supportMcpRoutes(handler) }
    }.start(wait = true)
}
