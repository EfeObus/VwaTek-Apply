package com.vwatek.apply.data.repository

import com.vwatek.apply.domain.model.*
import com.vwatek.apply.domain.repository.AuthError
import com.vwatek.apply.domain.repository.AuthRepository
import com.vwatek.apply.domain.repository.GoogleUserData
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import kotlin.js.json
import kotlinx.coroutines.await

/**
 * API-based Auth Repository that communicates with the backend
 * Uses localStorage only for caching the current session
 */
class ApiAuthRepository : AuthRepository {
    
    private val _authState = MutableStateFlow(AuthState())
    private val authKey = "vwatek_auth_session"
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    // Backend API URL - Use Cloud Run URL in production
    private val apiBaseUrl: String
        get() {
            val hostname = window.location.hostname
            return when {
                hostname == "localhost" || hostname == "127.0.0.1" -> "http://localhost:8090"
                hostname.contains("storage.googleapis.com") -> "https://vwatek-backend-21443684777.us-central1.run.app"
                else -> "https://vwatek-backend-21443684777.us-central1.run.app"
            }
        }
    
    init {
        loadCachedSession()
    }
    
    /**
     * Load cached session from localStorage
     */
    private fun loadCachedSession() {
        val stored = localStorage.getItem(authKey) ?: return
        try {
            val session = json.decodeFromString<CachedSession>(stored)
            
            // Check if session is expired
            val expiresAt = Instant.parse(session.expiresAt)
            if (Clock.System.now() >= expiresAt) {
                console.log("Cached session expired, clearing")
                localStorage.removeItem(authKey)
                return
            }
            
            // Restore session
            _authState.value = AuthState(
                isAuthenticated = true,
                user = session.user,
                accessToken = session.token
            )
            console.log("Session restored from cache for user: ${session.user.email}")
        } catch (e: Exception) {
            console.error("Error loading cached session: ${e.message}")
            localStorage.removeItem(authKey)
        }
    }
    
    /**
     * Cache session to localStorage
     */
    private fun cacheSession(user: User, token: String, expiresAt: String) {
        try {
            val session = CachedSession(
                user = user,
                token = token,
                expiresAt = expiresAt
            )
            localStorage.setItem(authKey, json.encodeToString(session))
            console.log("Session cached for user: ${user.email}")
        } catch (e: Exception) {
            console.error("Error caching session: ${e.message}")
        }
    }
    
    /**
     * Clear cached session
     */
    private fun clearCachedSession() {
        localStorage.removeItem(authKey)
    }
    
    override fun getAuthState(): Flow<AuthState> = _authState.asStateFlow()
    
    override suspend fun getCurrentUser(): User? = _authState.value.user
    
    override suspend fun registerWithEmail(data: RegistrationData): Result<User> {
        return try {
            console.log("Registering user with email: ${data.email}")
            
            val requestBody = json.encodeToString(RegisterRequest(
                email = data.email,
                password = data.password,
                firstName = data.firstName,
                lastName = data.lastName,
                phone = data.phone
            ))
            
            val response = fetch(
                "$apiBaseUrl/api/v1/auth/register",
                RequestInit(
                    method = "POST",
                    headers = json("Content-Type" to "application/json"),
                    body = requestBody
                )
            ).await()
            
            if (response.ok) {
                val responseText = response.text().await()
                val authResponse = json.decodeFromString<AuthApiResponse>(responseText)
                
                val user = authResponse.user.toUser()
                
                _authState.value = AuthState(
                    isAuthenticated = true,
                    user = user,
                    accessToken = authResponse.token
                )
                
                cacheSession(user, authResponse.token, authResponse.expiresAt)
                
                console.log("Registration successful for: ${user.email}")
                Result.success(user)
            } else {
                val errorText = response.text().await()
                console.error("Registration failed: $errorText")
                
                when (response.status.toInt()) {
                    409 -> Result.failure(AuthError.EmailAlreadyExists)
                    400 -> Result.failure(AuthError.InvalidEmail)
                    else -> Result.failure(Exception("Registration failed: $errorText"))
                }
            }
        } catch (e: Exception) {
            console.error("Registration error: ${e.message}")
            Result.failure(AuthError.NetworkError)
        }
    }
    
    override suspend fun loginWithEmail(email: String, password: String, rememberMe: Boolean): Result<User> {
        return try {
            console.log("Logging in user: $email")
            
            val requestBody = json.encodeToString(LoginRequest(
                email = email,
                password = password,
                rememberMe = rememberMe
            ))
            
            val response = fetch(
                "$apiBaseUrl/api/v1/auth/login",
                RequestInit(
                    method = "POST",
                    headers = json("Content-Type" to "application/json"),
                    body = requestBody
                )
            ).await()
            
            if (response.ok) {
                val responseText = response.text().await()
                val authResponse = json.decodeFromString<AuthApiResponse>(responseText)
                
                val user = authResponse.user.toUser()
                
                _authState.value = AuthState(
                    isAuthenticated = true,
                    user = user,
                    accessToken = authResponse.token
                )
                
                cacheSession(user, authResponse.token, authResponse.expiresAt)
                
                console.log("Login successful for: ${user.email}")
                Result.success(user)
            } else {
                val errorText = response.text().await()
                console.error("Login failed: $errorText")
                
                when (response.status.toInt()) {
                    401 -> Result.failure(AuthError.InvalidCredentials)
                    404 -> Result.failure(AuthError.AccountNotFound)
                    else -> Result.failure(Exception("Login failed: $errorText"))
                }
            }
        } catch (e: Exception) {
            console.error("Login error: ${e.message}")
            Result.failure(AuthError.NetworkError)
        }
    }
    
    override suspend fun loginWithGoogle(idToken: String, userInfo: GoogleUserData?): Result<User> {
        return try {
            console.log("Google Sign-In for: ${userInfo?.email}")
            
            if (userInfo == null) {
                return Result.failure(AuthError.GoogleSignInFailed)
            }
            
            val requestBody = json.encodeToString(GoogleAuthRequest(
                email = userInfo.email,
                firstName = userInfo.firstName,
                lastName = userInfo.lastName,
                profilePicture = userInfo.profilePicture
            ))
            
            val response = fetch(
                "$apiBaseUrl/api/v1/auth/google",
                RequestInit(
                    method = "POST",
                    headers = json("Content-Type" to "application/json"),
                    body = requestBody
                )
            ).await()
            
            if (response.ok) {
                val responseText = response.text().await()
                val authResponse = json.decodeFromString<AuthApiResponse>(responseText)
                
                val user = authResponse.user.toUser()
                
                _authState.value = AuthState(
                    isAuthenticated = true,
                    user = user,
                    accessToken = authResponse.token
                )
                
                cacheSession(user, authResponse.token, authResponse.expiresAt)
                
                console.log("Google Sign-In successful for: ${user.email}")
                Result.success(user)
            } else {
                val errorText = response.text().await()
                console.error("Google Sign-In failed: $errorText")
                Result.failure(AuthError.GoogleSignInFailed)
            }
        } catch (e: Exception) {
            console.error("Google Sign-In error: ${e.message}")
            Result.failure(AuthError.GoogleSignInFailed)
        }
    }
    
    override suspend fun loginWithLinkedIn(authCode: String): Result<User> {
        return try {
            console.log("LinkedIn Sign-In with auth code")
            
            // Get the redirect URI that was used for the auth flow
            val redirectUri = "${window.location.origin}/linkedin-callback"
            
            val requestBody = json.encodeToString(LinkedInAuthRequest(
                code = authCode,
                redirectUri = redirectUri
            ))
            
            val response = fetch(
                "$apiBaseUrl/api/v1/auth/linkedin",
                RequestInit(
                    method = "POST",
                    headers = json("Content-Type" to "application/json"),
                    body = requestBody
                )
            ).await()
            
            if (response.ok) {
                val responseText = response.text().await()
                val authResponse = json.decodeFromString<AuthApiResponse>(responseText)
                
                val user = authResponse.user.toUser()
                
                _authState.value = AuthState(
                    isAuthenticated = true,
                    user = user,
                    accessToken = authResponse.token
                )
                
                cacheSession(user, authResponse.token, authResponse.expiresAt)
                
                console.log("LinkedIn Sign-In successful for: ${user.email}")
                Result.success(user)
            } else {
                val errorText = response.text().await()
                console.error("LinkedIn Sign-In failed: $errorText")
                Result.failure(AuthError.LinkedInSignInFailed)
            }
        } catch (e: Exception) {
            console.error("LinkedIn Sign-In error: ${e.message}")
            Result.failure(AuthError.LinkedInSignInFailed)
        }
    }
    
    override suspend fun logout() {
        _authState.value = AuthState()
        clearCachedSession()
        console.log("User logged out")
    }
    
    override suspend fun updateProfile(user: User): Result<User> {
        // TODO: Implement backend profile update endpoint
        return Result.failure(Exception("Not implemented"))
    }
    
    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            console.log("Requesting password reset for: $email")
            
            val requestBody = json.encodeToString(ResetPasswordRequest(email = email))
            
            val response = fetch(
                "$apiBaseUrl/api/v1/auth/reset-password",
                RequestInit(
                    method = "POST",
                    headers = json("Content-Type" to "application/json"),
                    body = requestBody
                )
            ).await()
            
            if (response.ok) {
                console.log("Password reset request successful")
                Result.success(Unit)
            } else {
                val errorText = response.text().await()
                console.error("Password reset failed: $errorText")
                Result.failure(Exception("Password reset request failed"))
            }
        } catch (e: Exception) {
            console.error("Password reset error: ${e.message}")
            Result.failure(AuthError.NetworkError)
        }
    }
    
    override suspend fun isEmailAvailable(email: String): Boolean {
        // TODO: Implement backend email check endpoint
        return true
    }
    
    override suspend fun sendVerificationEmail(email: String): Result<Unit> {
        return Result.failure(Exception("Not implemented"))
    }
    
    override suspend fun verifyEmail(token: String): Result<Unit> {
        return Result.failure(Exception("Not implemented"))
    }
    
    override suspend fun isEmailVerified(userId: String): Boolean {
        return _authState.value.user?.emailVerified ?: false
    }
}

// API Request/Response DTOs
@Serializable
private data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val phone: String? = null
)

@Serializable
private data class LoginRequest(
    val email: String,
    val password: String,
    val rememberMe: Boolean = false
)

@Serializable
private data class AuthApiResponse(
    val user: UserApiResponse,
    val token: String,
    val expiresAt: String
)

@Serializable
private data class UserApiResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String? = null,
    val authProvider: String,
    val emailVerified: Boolean,
    val createdAt: String
) {
    fun toUser(): User = User(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        phone = phone,
        address = null,
        profileImageUrl = null,
        authProvider = when (authProvider) {
            "EMAIL" -> AuthProvider.EMAIL
            "GOOGLE" -> AuthProvider.GOOGLE
            "LINKEDIN" -> AuthProvider.LINKEDIN
            else -> AuthProvider.EMAIL
        },
        linkedInProfileUrl = null,
        emailVerified = emailVerified,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(createdAt)
    )
}

@Serializable
private data class CachedSession(
    val user: User,
    val token: String,
    val expiresAt: String
)

@Serializable
private data class GoogleAuthRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val profilePicture: String? = null
)

@Serializable
private data class LinkedInAuthRequest(
    val code: String,
    val redirectUri: String
)

@Serializable
private data class ResetPasswordRequest(
    val email: String
)

// Helper function for fetch
private external fun fetch(url: String, init: RequestInit): kotlin.js.Promise<Response>
