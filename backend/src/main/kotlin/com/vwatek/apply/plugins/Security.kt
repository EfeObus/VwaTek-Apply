package com.vwatek.apply.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

/**
 * JWT Authentication Configuration
 * Secures API endpoints with JSON Web Token authentication
 */
fun Application.configureSecurity() {
    val jwtSecret = System.getenv("JWT_SECRET") ?: "vwatek-apply-secret-key-change-in-production"
    val jwtIssuer = System.getenv("JWT_ISSUER") ?: "vwatek-apply"
    val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "vwatek-apply-users"
    val jwtRealm = "VwaTek Apply API"
    
    val jwtVerifier = JWT.require(Algorithm.HMAC256(jwtSecret))
        .withAudience(jwtAudience)
        .withIssuer(jwtIssuer)
        .build()
    
    install(Authentication) {
        // Primary JWT configuration - used by most routes
        jwt("jwt") {
            realm = jwtRealm
            verifier(jwtVerifier)
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Token is not valid or has expired")
                )
            }
        }
        
        // Legacy auth-jwt configuration - used by privacy routes
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(jwtVerifier)
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Token is not valid or has expired")
                )
            }
        }
    }
}

/**
 * Extension to get user ID from JWT principal
 */
fun JWTPrincipal.userId(): String? = payload.getClaim("userId").asString()

/**
 * Extension to get user email from JWT principal
 */
fun JWTPrincipal.userEmail(): String? = payload.getClaim("email").asString()
