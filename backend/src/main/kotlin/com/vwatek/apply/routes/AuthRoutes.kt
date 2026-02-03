package com.vwatek.apply.routes

import com.vwatek.apply.db.tables.UsersTable
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest
import java.util.*
import kotlin.time.Duration.Companion.days

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val rememberMe: Boolean = false
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val phone: String? = null
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String?,
    val authProvider: String,
    val emailVerified: Boolean,
    val createdAt: String
)

@Serializable
data class AuthResponse(
    val user: UserResponse,
    val token: String,
    val expiresAt: String
)

@Serializable
data class LinkedInAuthRequest(
    val code: String,
    val redirectUri: String
)

@Serializable
data class GoogleAuthRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val profilePicture: String? = null
)

@Serializable
data class ResetPasswordRequest(
    val email: String
)

fun Route.authRoutes() {
    route("/auth") {
        // Register
        post("/register") {
            val request = call.receive<RegisterRequest>()
            
            // Validate email
            if (!request.email.contains("@")) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid email format"))
                return@post
            }
            
            // Check if email exists
            val exists = transaction {
                UsersTable.select { UsersTable.email eq request.email.lowercase() }.count() > 0
            }
            
            if (exists) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "Email already registered"))
                return@post
            }
            
            // Hash password
            val hashedPassword = hashPassword(request.password)
            val userId = UUID.randomUUID().toString()
            val now = Clock.System.now()
            
            transaction {
                UsersTable.insert {
                    it[id] = userId
                    it[email] = request.email.lowercase()
                    it[password] = hashedPassword
                    it[firstName] = request.firstName
                    it[lastName] = request.lastName
                    it[phone] = request.phone
                    it[authProvider] = "EMAIL"
                    it[emailVerified] = false
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
            
            val token = generateToken()
            val expiresAt = now.plus(30.days)
            
            call.respond(HttpStatusCode.Created, AuthResponse(
                user = UserResponse(
                    id = userId,
                    email = request.email,
                    firstName = request.firstName,
                    lastName = request.lastName,
                    phone = request.phone,
                    authProvider = "EMAIL",
                    emailVerified = false,
                    createdAt = now.toString()
                ),
                token = token,
                expiresAt = expiresAt.toString()
            ))
        }
        
        // Login
        post("/login") {
            val request = call.receive<LoginRequest>()
            
            val user = transaction {
                UsersTable.select { UsersTable.email eq request.email.lowercase() }.firstOrNull()
            }
            
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid email or password"))
                return@post
            }
            
            val storedPassword = user[UsersTable.password]
            if (storedPassword == null || !verifyPassword(request.password, storedPassword)) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid email or password"))
                return@post
            }
            
            val token = generateToken()
            val now = Clock.System.now()
            val expiryDays = if (request.rememberMe) 30 else 1
            val expiresAt = now.plus(expiryDays.days)
            
            call.respond(AuthResponse(
                user = UserResponse(
                    id = user[UsersTable.id],
                    email = user[UsersTable.email],
                    firstName = user[UsersTable.firstName],
                    lastName = user[UsersTable.lastName],
                    phone = user[UsersTable.phone],
                    authProvider = user[UsersTable.authProvider],
                    emailVerified = user[UsersTable.emailVerified],
                    createdAt = user[UsersTable.createdAt].toString()
                ),
                token = token,
                expiresAt = expiresAt.toString()
            ))
        }
        
        // Get current user
        get("/me") {
            // In production, extract user from JWT token
            val userId = call.request.headers["X-User-Id"]
            
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
                return@get
            }
            
            val user = transaction {
                UsersTable.select { UsersTable.id eq userId }.firstOrNull()
            }
            
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                return@get
            }
            
            call.respond(UserResponse(
                id = user[UsersTable.id],
                email = user[UsersTable.email],
                firstName = user[UsersTable.firstName],
                lastName = user[UsersTable.lastName],
                phone = user[UsersTable.phone],
                authProvider = user[UsersTable.authProvider],
                emailVerified = user[UsersTable.emailVerified],
                createdAt = user[UsersTable.createdAt].toString()
            ))
        }
        
        // Reset Password
        post("/reset-password") {
            val request = call.receive<ResetPasswordRequest>()
            
            // Check if user exists
            val user = transaction {
                UsersTable.select { UsersTable.email eq request.email.lowercase() }.firstOrNull()
            }
            
            // Always return success to prevent email enumeration attacks
            // In production, this would send an email with a reset link
            if (user != null) {
                // TODO: In production, generate a reset token, store it, and send email
                // For now, just log that a reset was requested
                call.application.log.info("Password reset requested for: ${request.email}")
            }
            
            // Return success regardless of whether user exists
            call.respond(mapOf(
                "message" to "If an account exists with this email, you will receive password reset instructions shortly.",
                "success" to true
            ))
        }
        
        // Google Sign-In
        post("/google") {
            val request = call.receive<GoogleAuthRequest>()
            
            // Check if user exists
            val existingUser = transaction {
                UsersTable.select { UsersTable.email eq request.email.lowercase() }.firstOrNull()
            }
            
            val now = Clock.System.now()
            val token = generateToken()
            val expiresAt = now.plus(30.days)
            
            if (existingUser != null) {
                // User exists, return their info
                call.respond(AuthResponse(
                    user = UserResponse(
                        id = existingUser[UsersTable.id],
                        email = existingUser[UsersTable.email],
                        firstName = existingUser[UsersTable.firstName],
                        lastName = existingUser[UsersTable.lastName],
                        phone = existingUser[UsersTable.phone],
                        authProvider = existingUser[UsersTable.authProvider],
                        emailVerified = existingUser[UsersTable.emailVerified],
                        createdAt = existingUser[UsersTable.createdAt].toString()
                    ),
                    token = token,
                    expiresAt = expiresAt.toString()
                ))
            } else {
                // Create new user
                val userId = UUID.randomUUID().toString()
                
                transaction {
                    UsersTable.insert {
                        it[id] = userId
                        it[email] = request.email.lowercase()
                        it[password] = null
                        it[firstName] = request.firstName
                        it[lastName] = request.lastName
                        it[authProvider] = "GOOGLE"
                        it[emailVerified] = true
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                }
                
                call.respond(HttpStatusCode.Created, AuthResponse(
                    user = UserResponse(
                        id = userId,
                        email = request.email,
                        firstName = request.firstName,
                        lastName = request.lastName,
                        phone = null,
                        authProvider = "GOOGLE",
                        emailVerified = true,
                        createdAt = now.toString()
                    ),
                    token = token,
                    expiresAt = expiresAt.toString()
                ))
            }
        }
        
        // LinkedIn OAuth
        post("/linkedin") {
            val request = call.receive<LinkedInAuthRequest>()
            
            // Exchange authorization code for access token
            val linkedInConfig = LinkedInConfig.fromEnvironment()
            
            try {
                // Step 1: Exchange code for access token
                val tokenResponse = exchangeLinkedInCode(request.code, request.redirectUri, linkedInConfig)
                
                // Step 2: Fetch user profile using access token
                val userProfile = fetchLinkedInProfile(tokenResponse.accessToken)
                
                // Step 3: Check if user exists or create new one
                val existingUser = transaction {
                    UsersTable.select { UsersTable.email eq userProfile.email.lowercase() }.firstOrNull()
                }
                
                val now = Clock.System.now()
                val token = generateToken()
                val expiresAt = now.plus(30.days)
                
                if (existingUser != null) {
                    // Update LinkedIn profile URL if available
                    transaction {
                        UsersTable.update({ UsersTable.id eq existingUser[UsersTable.id] }) {
                            it[linkedInProfileUrl] = userProfile.profileUrl
                            it[updatedAt] = now
                        }
                    }
                    
                    call.respond(AuthResponse(
                        user = UserResponse(
                            id = existingUser[UsersTable.id],
                            email = existingUser[UsersTable.email],
                            firstName = existingUser[UsersTable.firstName],
                            lastName = existingUser[UsersTable.lastName],
                            phone = existingUser[UsersTable.phone],
                            authProvider = existingUser[UsersTable.authProvider],
                            emailVerified = existingUser[UsersTable.emailVerified],
                            createdAt = existingUser[UsersTable.createdAt].toString()
                        ),
                        token = token,
                        expiresAt = expiresAt.toString()
                    ))
                } else {
                    // Create new user
                    val userId = UUID.randomUUID().toString()
                    
                    transaction {
                        UsersTable.insert {
                            it[id] = userId
                            it[email] = userProfile.email.lowercase()
                            it[password] = null
                            it[firstName] = userProfile.firstName
                            it[lastName] = userProfile.lastName
                            it[authProvider] = "LINKEDIN"
                            it[emailVerified] = true
                            it[linkedInProfileUrl] = userProfile.profileUrl
                            it[createdAt] = now
                            it[updatedAt] = now
                        }
                    }
                    
                    call.respond(HttpStatusCode.Created, AuthResponse(
                        user = UserResponse(
                            id = userId,
                            email = userProfile.email,
                            firstName = userProfile.firstName,
                            lastName = userProfile.lastName,
                            phone = null,
                            authProvider = "LINKEDIN",
                            emailVerified = true,
                            createdAt = now.toString()
                        ),
                        token = token,
                        expiresAt = expiresAt.toString()
                    ))
                }
            } catch (e: Exception) {
                call.application.log.error("LinkedIn OAuth error", e)
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "LinkedIn authentication failed")))
            }
        }
    }
}

private fun hashPassword(password: String): String {
    val salt = UUID.randomUUID().toString()
    val md = MessageDigest.getInstance("SHA-256")
    val hash = md.digest("$salt:$password".toByteArray()).joinToString("") { "%02x".format(it) }
    return "$salt:$hash"
}

private fun verifyPassword(password: String, storedHash: String): Boolean {
    val parts = storedHash.split(":")
    if (parts.size != 2) return false
    val salt = parts[0]
    val hash = parts[1]
    
    val md = MessageDigest.getInstance("SHA-256")
    val computedHash = md.digest("$salt:$password".toByteArray()).joinToString("") { "%02x".format(it) }
    return hash == computedHash
}

private fun generateToken(): String {
    return UUID.randomUUID().toString() + UUID.randomUUID().toString().replace("-", "")
}

// LinkedIn OAuth Configuration
data class LinkedInConfig(
    val clientId: String,
    val clientSecret: String
) {
    companion object {
        fun fromEnvironment(): LinkedInConfig {
            return LinkedInConfig(
                clientId = System.getenv("LINKEDIN_CLIENT_ID") ?: "86zpbbqqqa32et",
                clientSecret = System.getenv("LINKEDIN_CLIENT_SECRET") ?: throw IllegalStateException("LINKEDIN_CLIENT_SECRET not configured")
            )
        }
    }
}

// LinkedIn API response models
@Serializable
data class LinkedInTokenResponse(
    val access_token: String,
    val expires_in: Int,
    val scope: String? = null,
    val token_type: String? = null
) {
    val accessToken: String get() = access_token
}

data class LinkedInUserProfile(
    val email: String,
    val firstName: String,
    val lastName: String,
    val profileUrl: String? = null,
    val pictureUrl: String? = null
)

// Exchange authorization code for access token
private suspend fun exchangeLinkedInCode(
    code: String,
    redirectUri: String,
    config: LinkedInConfig
): LinkedInTokenResponse {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    try {
        val response = client.post("https://www.linkedin.com/oauth/v2/accessToken") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(Parameters.build {
                append("grant_type", "authorization_code")
                append("code", code)
                append("redirect_uri", redirectUri)
                append("client_id", config.clientId)
                append("client_secret", config.clientSecret)
            }.formUrlEncode())
        }
        
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            throw Exception("LinkedIn token exchange failed: $errorBody")
        }
        
        return response.body<LinkedInTokenResponse>()
    } finally {
        client.close()
    }
}

// Fetch user profile from LinkedIn
private suspend fun fetchLinkedInProfile(accessToken: String): LinkedInUserProfile {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    try {
        // Use OpenID Connect userinfo endpoint (works with openid, profile, email scopes)
        val response = client.get("https://api.linkedin.com/v2/userinfo") {
            header("Authorization", "Bearer $accessToken")
        }
        
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            throw Exception("LinkedIn profile fetch failed: $errorBody")
        }
        
        val jsonParser = Json { ignoreUnknownKeys = true }
        val body = response.bodyAsText()
        val userInfo = jsonParser.parseToJsonElement(body).jsonObject
        
        return LinkedInUserProfile(
            email = userInfo["email"]?.jsonPrimitive?.contentOrNull 
                ?: throw Exception("Email not provided by LinkedIn"),
            firstName = userInfo["given_name"]?.jsonPrimitive?.contentOrNull ?: "",
            lastName = userInfo["family_name"]?.jsonPrimitive?.contentOrNull ?: "",
            pictureUrl = userInfo["picture"]?.jsonPrimitive?.contentOrNull,
            profileUrl = null // LinkedIn userinfo doesn't provide profile URL
        )
    } finally {
        client.close()
    }
}
