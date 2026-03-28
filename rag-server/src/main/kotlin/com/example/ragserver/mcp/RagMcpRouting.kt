package com.example.ragserver.mcp

import com.example.aichallengeapp.core.mcp.model.JsonRpcRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import java.util.UUID


fun Routing.ragMcpRoutes(handler: RagMcpRequestHandler) {
    post("/mcp") {
        val request = call.receive<JsonRpcRequest>()
        if (request.method == "notifications/initialized") {
            call.respond(HttpStatusCode.Accepted, "")
            return@post
        }
        val response = handler.handle(request)
        if (request.method == "initialize") {
            call.response.headers.append("Mcp-Session-Id", UUID.randomUUID().toString())
        }
        call.respond(response)
    }
}
