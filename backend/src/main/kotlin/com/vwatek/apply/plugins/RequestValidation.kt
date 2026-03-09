package com.vwatek.apply.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("RequestValidation")

/**
 * Configures request body size limits and basic input sanitization.
 * Prevents oversized payloads and common injection patterns.
 */
fun Application.configureRequestValidation() {
    // Max request body size: 1 MB for most endpoints, prevents DoS via large payloads
    val maxBodySize = 1_048_576L // 1 MB
    // Larger limit for file-upload endpoints (resumes, etc.)
    val maxFileUploadSize = 10_485_760L // 10 MB

    intercept(ApplicationCallPipeline.Plugins) {
        val contentLength = call.request.contentLength() ?: 0L
        val path = call.request.path()

        val limit = if (path.contains("/resumes") || path.contains("/upload") || path.contains("/documents")) {
            maxFileUploadSize
        } else {
            maxBodySize
        }

        if (contentLength > limit) {
            logger.warn("Request body too large: $contentLength bytes for $path (limit: $limit)")
            call.respond(
                HttpStatusCode.PayloadTooLarge,
                mapOf("error" to "Request body too large. Maximum size: ${limit / 1024}KB")
            )
            finish()
            return@intercept
        }
    }
}

/**
 * Sanitizes user-supplied text input — strips HTML tags and null bytes.
 * Call this on any user text before storing in the database.
 */
fun sanitizeInput(input: String): String {
    return input
        .replace(Regex("<[^>]*>"), "")        // Strip HTML tags
        .replace("\u0000", "")                 // Remove null bytes
        .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "") // Remove control chars (except \t, \n, \r)
        .trim()
}
