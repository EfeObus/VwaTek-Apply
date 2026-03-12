package com.vwatek.apply.plugins

import com.vwatek.apply.config.DatabaseConfig
import com.vwatek.apply.routes.*
import com.vwatek.apply.services.AIService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
        // Health check endpoint
        get("/health") {
            call.respond(HealthResponse(
                status = "healthy",
                database = "postgresql",
                timestamp = System.currentTimeMillis()
            ))
        }
        
        // LinkedIn OAuth callback for mobile apps
        get("/linkedin-callback") {
            val code = call.request.queryParameters["code"]
            val error = call.request.queryParameters["error"]
            val errorDescription = call.request.queryParameters["error_description"]
            
            if (error != null) {
                // Error from LinkedIn
                call.respondText(
                    contentType = io.ktor.http.ContentType.Text.Html,
                    text = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <title>LinkedIn Login Failed</title>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <style>
                                body { font-family: -apple-system, system-ui, sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; background: #f3f2ef; }
                                .container { text-align: center; padding: 40px; background: white; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15); max-width: 400px; }
                                h1 { color: #c73b22; margin-bottom: 16px; }
                                p { color: #666; margin-bottom: 24px; }
                                a { color: #0a66c2; text-decoration: none; font-weight: 500; }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <h1>Login Failed</h1>
                                <p>${errorDescription ?: "LinkedIn authentication was not successful."}</p>
                                <a href="vwatekapply://linkedin-callback?error=$error">Return to App</a>
                            </div>
                        </body>
                        </html>
                    """.trimIndent()
                )
                return@get
            }
            
            if (code != null) {
                // Success - redirect to app with deep link
                call.respondText(
                    contentType = io.ktor.http.ContentType.Text.Html,
                    text = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <title>LinkedIn Login Success</title>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <style>
                                body { font-family: -apple-system, system-ui, sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; background: linear-gradient(135deg, #0a66c2 0%, #004182 100%); }
                                .container { text-align: center; padding: 40px; background: white; border-radius: 12px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); max-width: 400px; }
                                h1 { color: #0a66c2; margin-bottom: 16px; }
                                p { color: #666; margin-bottom: 24px; }
                                .spinner { width: 40px; height: 40px; border: 4px solid #e1e1e1; border-top-color: #0a66c2; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 24px; }
                                @keyframes spin { to { transform: rotate(360deg); } }
                                a.btn { display: inline-block; background: #0a66c2; color: white; padding: 12px 24px; border-radius: 24px; text-decoration: none; font-weight: 500; }
                                a.btn:hover { background: #004182; }
                            </style>
                            <script>
                                // Try to redirect to app automatically
                                window.location.href = 'vwatekapply://linkedin-callback?code=$code';
                                // Fallback after 2 seconds
                                setTimeout(function() {
                                    document.getElementById('manual-link').style.display = 'block';
                                    document.getElementById('spinner').style.display = 'none';
                                }, 2000);
                            </script>
                        </head>
                        <body>
                            <div class="container">
                                <h1>Login Successful!</h1>
                                <div id="spinner" class="spinner"></div>
                                <p>Redirecting you back to the app...</p>
                                <div id="manual-link" style="display: none;">
                                    <p>If you're not redirected automatically:</p>
                                    <a class="btn" href="vwatekapply://linkedin-callback?code=$code">Open VwaTek Apply</a>
                                </div>
                            </div>
                        </body>
                        </html>
                    """.trimIndent()
                )
            } else {
                call.respondText(
                    contentType = io.ktor.http.ContentType.Text.Html,
                    text = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <title>Invalid Request</title>
                            <style>
                                body { font-family: -apple-system, system-ui, sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; }
                                .container { text-align: center; padding: 40px; }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <h1>Invalid Request</h1>
                                <p>Missing authorization code</p>
                            </div>
                        </body>
                        </html>
                    """.trimIndent()
                )
            }
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
                    EndpointInfo("/api/v1/interviews", "Interview prep with AI feedback"),
                    EndpointInfo("/api/v1/tracker", "Track job applications"),
                    EndpointInfo("/api/v1/noc", "Canadian NOC codes database"),
                    EndpointInfo("/api/v1/jobbank", "Job Bank Canada integration")
                ),
                documentation = "https://github.com/EfeObus/VwaTek-Apply"
            ))
        }
        
        // API routes
        route("/api/v1") {
            // Create HTTP client for AI service
            val httpClient = HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    })
                }
            }
            val aiService = AIService(httpClient)
            
            rateLimit(RateLimitName("auth")) {
                authRoutes()
            }
            resumeRoutes()
            coverLetterRoutes()
            interviewRoutes()
            rateLimit(RateLimitName("ai")) {
                aiRoutes(aiService)
            }
            
            // Phase 2: Job Tracker
            jobTrackerRoutes()
            
            // Phase 3: NOC Codes
            nocRoutes()
            
            // Phase 3: Job Bank Canada Integration
            jobBankRoutes()
            
            // Phase 3: Notifications
            notificationRoutes()
            
            // Phase 4: Premium & Monetization
            rateLimit(RateLimitName("subscription")) {
                subscriptionRoutes(httpClient)
            }
            salaryRoutes(httpClient)
            
            // Phase 5: LinkedIn Optimizer
            linkedInRoutes(aiService)
            
            // Phase 5: Enterprise & Organizations
            organizationRoutes()
        }
        
        // Sync routes (separate from versioned API for flexibility)
        authenticate("jwt") {
            syncRoutes()
        }
        
        // Privacy routes for PIPEDA compliance
        authenticate("jwt") {
            privacyRoutes()
        }
        
        // Serve web frontend from embedded JAR resources
        staticResources("/", "web") {
            default("index.html")
        }
    }
}
