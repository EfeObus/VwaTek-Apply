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

@Serializable
data class ApiInfoResponse(
    val name: String,
    val version: String,
    val description: String,
    val endpoints: List<EndpointInfo>,
    val documentation: String
)

@Serializable
data class EndpointInfo(
    val path: String,
    val description: String
)

fun Application.configureRouting() {
    routing {
        // Root endpoint - API info
        get("/") {
            call.respond(ApiInfoResponse(
                name = "VwaTek Apply API",
                version = "1.0.0",
                description = "AI-powered job application assistant - Resume optimization, cover letter generation, and interview preparation",
                endpoints = listOf(
                    EndpointInfo("/health", "Health check endpoint"),
                    EndpointInfo("/api/v1/auth", "Authentication endpoints"),
                    EndpointInfo("/api/v1/resumes", "Resume management and AI analysis"),
                    EndpointInfo("/api/v1/cover-letters", "AI-powered cover letter generation"),
                    EndpointInfo("/api/v1/interviews", "Interview preparation and practice")
                ),
                documentation = "https://github.com/EfeObus/VwaTek-Apply"
            ))
        }
        
        // Health check endpoint
        get("/health") {
            call.respond(HealthResponse(
                status = "healthy",
                database = if (DatabaseConfig.isUsingCloudSQL()) "cloud-sql" else "local",
                timestamp = System.currentTimeMillis()
            ))
        }
        
        // API info endpoint
        get("/api/v1") {
            call.respond(ApiInfoResponse(
                name = "VwaTek Apply API",
                version = "1.0.0",
                description = "AI-powered job application assistant",
                endpoints = listOf(
                    EndpointInfo("/api/v1/auth", "Authentication - login, register, logout"),
                    EndpointInfo("/api/v1/resumes", "Resume CRUD and AI optimization"),
                    EndpointInfo("/api/v1/cover-letters", "Generate tailored cover letters"),
                    EndpointInfo("/api/v1/interviews", "Interview prep with AI feedback")
                ),
                documentation = "https://github.com/EfeObus/VwaTek-Apply"
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
