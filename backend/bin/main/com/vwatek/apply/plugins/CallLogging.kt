package com.vwatek.apply.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.origin
import io.ktor.server.request.*
import org.slf4j.event.Level
import java.util.UUID

fun Application.configureCallLogging() {
    // Generate unique request IDs for tracing
    install(CallId) {
        header("X-Request-Id")
        generate { UUID.randomUUID().toString().take(12) }
        verify { it.isNotBlank() }
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/api") || call.request.path().startsWith("/sync") }
        callIdMdc("request_id")
        // Structured JSON-like log format with request ID and user context
        format { call ->
            val status = call.response.status()?.value ?: 0
            val method = call.request.httpMethod.value
            val path = call.request.path()
            val requestId = call.callId ?: "-"
            val userId = call.principal<JWTPrincipal>()?.subject ?: call.request.headers["X-User-Id"] ?: "-"
            val duration = call.processingTimeMillis()
            val ip = call.request.origin.remoteHost
            "{\"request_id\":\"$requestId\",\"method\":\"$method\",\"path\":\"$path\",\"status\":$status,\"user_id\":\"$userId\",\"duration_ms\":$duration,\"ip\":\"$ip\"}"
        }
    }
}
