package com.vwatek.apply.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.vwatek.apply.db.tables.UsersTable
import com.vwatek.apply.services.EmailService
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
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.vwatek.apply.auth.requireUserId
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
            if (!request.email.contains("@") || !request.email.contains(".")) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid email format"))
                return@post
            }
            
            // Validate password strength
            val passwordErrors = validatePasswordStrength(request.password)
            if (passwordErrors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "error" to "Password does not meet requirements",
                    "details" to passwordErrors
                ))
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
            
            // Sanitize user-supplied text
            val safeFirstName = com.vwatek.apply.plugins.sanitizeInput(request.firstName)
            val safeLastName = com.vwatek.apply.plugins.sanitizeInput(request.lastName)
            val safePhone = request.phone?.let { com.vwatek.apply.plugins.sanitizeInput(it) }
            
            transaction {
                UsersTable.insert {
                    it[id] = userId
                    it[email] = request.email.lowercase()
                    it[password] = hashedPassword
                    it[firstName] = safeFirstName
                    it[lastName] = safeLastName
                    it[phone] = safePhone
                    it[authProvider] = "EMAIL"
                    it[emailVerified] = false
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
            
            val token = generateJwtToken(userId, request.email.lowercase(), 30)
            val expiresAt = now.plus(30.days)
            
            // Send welcome email asynchronously
            if (EmailService.isConfigured()) {
                try {
                    EmailService.sendWelcomeEmail(
                        toEmail = request.email,
                        userName = request.firstName
                    ).onFailure { e ->
                        call.application.log.error("Failed to send welcome email: ${e.message}")
                    }
                } catch (e: Exception) {
                    call.application.log.error("Error sending welcome email: ${e.message}")
                }
            }
            
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
            
            val now = Clock.System.now()
            val expiryDays = if (request.rememberMe) 30 else 1
            val token = generateJwtToken(user[UsersTable.id], user[UsersTable.email], expiryDays)
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
        
        // Token Refresh — exchange a valid (or recently expired) JWT for a new one
        authenticate("jwt") {
            post("/refresh") {
                val userId = call.requireUserId() ?: return@post
                
                val user = transaction {
                    UsersTable.select { UsersTable.id eq userId }.firstOrNull()
                }
                
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not found"))
                    return@post
                }
                
                val now = Clock.System.now()
                val token = generateJwtToken(user[UsersTable.id], user[UsersTable.email], 30)
                val expiresAt = now.plus(30.days)
                
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
        }
        
        // Get current user
        get("/me") {
            // Extract user ID from JWT Bearer token, fallback to X-User-Id header for backward compatibility
            val userId = extractUserIdFromToken(call) ?: call.request.headers["X-User-Id"]
            
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
            if (user != null && EmailService.isConfigured()) {
                val resetToken = UUID.randomUUID().toString()
                val userName = "${user[UsersTable.firstName]} ${user[UsersTable.lastName]}"
                
                // Store reset token in database (expires in 1 hour)
                // For now, we'll include token in the URL - in production, store in DB with expiry
                val resetLink = "https://storage.googleapis.com/vwatek-apply-frontend/index.html?reset_token=$resetToken&email=${request.email}"
                
                // Send email asynchronously
                try {
                    EmailService.sendPasswordResetEmail(
                        toEmail = request.email,
                        userName = userName,
                        resetToken = resetToken,
                        resetLink = resetLink
                    ).onFailure { e ->
                        call.application.log.error("Failed to send password reset email: ${e.message}")
                    }
                    call.application.log.info("Password reset email sent to: ${request.email}")
                } catch (e: Exception) {
                    call.application.log.error("Error sending password reset email: ${e.message}")
                }
            } else if (user != null) {
                call.application.log.info("Password reset requested but email service not configured for: ${request.email}")
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
            val expiresAt = now.plus(30.days)
            
            if (existingUser != null) {
                val token = generateJwtToken(existingUser[UsersTable.id], existingUser[UsersTable.email], 30)
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
                
                val token = generateJwtToken(userId, request.email.lowercase(), 30)
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
                val expiresAt = now.plus(30.days)
                
                if (existingUser != null) {
                    val token = generateJwtToken(existingUser[UsersTable.id], existingUser[UsersTable.email], 30)
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
                    
                    val token = generateJwtToken(userId, userProfile.email.lowercase(), 30)
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

private fun validatePasswordStrength(password: String): List<String> {
    val errors = mutableListOf<String>()
    if (password.length < 8) errors.add("Password must be at least 8 characters")
    if (!password.any { it.isUpperCase() }) errors.add("Password must contain at least one uppercase letter")
    if (!password.any { it.isLowerCase() }) errors.add("Password must contain at least one lowercase letter")
    if (!password.any { it.isDigit() }) errors.add("Password must contain at least one digit")
    if (!password.any { !it.isLetterOrDigit() }) errors.add("Password must contain at least one special character")
    return errors
}

private fun hashPassword(password: String): String {
    return at.favre.lib.crypto.bcrypt.BCrypt.withDefaults()
        .hashToString(12, password.toCharArray())
}

private fun verifyPassword(password: String, storedHash: String): Boolean {
    // Support both bcrypt and legacy SHA-256 hashes for migration
    return if (storedHash.startsWith("\$2")) {
        // bcrypt hash
        at.favre.lib.crypto.bcrypt.BCrypt.verifyer()
            .verify(password.toCharArray(), storedHash)
            .verified
    } else {
        // Legacy SHA-256 hash (salt:hash format) — for backward compatibility
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        val salt = parts[0]
        val hash = parts[1]
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val computedHash = md.digest("$salt:$password".toByteArray()).joinToString("") { "%02x".format(it) }
        hash == computedHash
    }
}

/**
 * Extract user ID from JWT Bearer token in the Authorization header.
 * Returns null if no valid token is present.
 */
private fun extractUserIdFromToken(call: io.ktor.server.application.ApplicationCall): String? {
    val authHeader = call.request.headers[HttpHeaders.Authorization] ?: return null
    if (!authHeader.startsWith("Bearer ", ignoreCase = true)) return null
    val token = authHeader.removePrefix("Bearer ").removePrefix("bearer ").trim()
    
    return try {
        val jwtSecret = System.getenv("JWT_SECRET") ?: "vwatek-apply-secret-key-change-in-production"
        val jwtIssuer = System.getenv("JWT_ISSUER") ?: "vwatek-apply"
        val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "vwatek-apply-users"
        
        val verifier = JWT.require(Algorithm.HMAC256(jwtSecret))
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .build()
        
        val decoded = verifier.verify(token)
        decoded.getClaim("userId").asString()
    } catch (e: Exception) {
        null
    }
}

/**
 * Generate a proper JWT token signed with HMAC256.
 * Includes userId, email, audience, issuer, and expiry claims
 * so that the Ktor authenticate("jwt") block can validate it.
 */
private fun generateJwtToken(userId: String, email: String, expiryDays: Int = 30): String {
    val jwtSecret = System.getenv("JWT_SECRET") ?: "vwatek-apply-secret-key-change-in-production"
    val jwtIssuer = System.getenv("JWT_ISSUER") ?: "vwatek-apply"
    val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "vwatek-apply-users"
    
    return JWT.create()
        .withAudience(jwtAudience)
        .withIssuer(jwtIssuer)
        .withSubject(userId)
        .withClaim("userId", userId)
        .withClaim("email", email)
        .withExpiresAt(java.util.Date(System.currentTimeMillis() + expiryDays.toLong() * 24 * 60 * 60 * 1000))
        .sign(Algorithm.HMAC256(jwtSecret))
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
