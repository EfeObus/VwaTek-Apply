package com.vwatek.apply.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

/**
 * Extension function to require authentication and return the user ID.
 * Returns null if authentication fails, and sends an Unauthorized response.
 */
suspend fun ApplicationCall.requireAuth(): String? {
    val principal = principal<JWTPrincipal>()
    return if (principal != null) {
        principal.subject ?: principal.payload.getClaim("userId")?.asString()
    } else {
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
        null
    }
}

/**
 * Extract userId from JWT principal with X-User-Id header fallback.
 * Use inside authenticate("jwt") blocks for backward compatibility during migration.
 * Returns null and responds with 401 if no userId found.
 */
suspend fun ApplicationCall.requireUserId(): String? {
    // Try JWT principal first (preferred)
    val principal = principal<JWTPrincipal>()
    val userId = principal?.subject
        ?: principal?.payload?.getClaim("userId")?.asString()
        ?: request.headers["X-User-Id"]
    
    if (userId == null) {
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not authenticated"))
    }
    return userId
}
