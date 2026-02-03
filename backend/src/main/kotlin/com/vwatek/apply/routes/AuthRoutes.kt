package com.vwatek.apply.routes

import com.vwatek.apply.db.tables.UsersTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
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
