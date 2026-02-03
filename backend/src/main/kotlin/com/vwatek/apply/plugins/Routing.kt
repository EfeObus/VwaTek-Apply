package com.vwatek.apply.plugins

import com.vwatek.apply.config.DatabaseConfig
import com.vwatek.apply.routes.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val database: String,
    val timestamp: Long
)

fun Application.configureRouting() {
    routing {
        // Health check endpoint
        get("/health") {
            call.respond(HealthResponse(
                status = "healthy",
                database = if (DatabaseConfig.isUsingCloudSQL()) "cloud-sql" else "local",
                timestamp = System.currentTimeMillis()
            ))
        }
        
        // API routes
        route("/api/v1") {
            authRoutes()
            resumeRoutes()
            coverLetterRoutes()
            interviewRoutes()
        }
    }
}
