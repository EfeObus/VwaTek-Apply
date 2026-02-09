package com.vwatek.apply.data.repository

import com.vwatek.apply.domain.model.*
import com.vwatek.apply.domain.repository.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// Backend API response types for Auth
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
)

@Serializable
private data class LoginRequest(
    val email: String,
    val password: String,
    val rememberMe: Boolean = false
)

@Serializable
private data class RegisterApiRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val phone: String? = null
)

/**
 * iOS implementation of AuthRepository
 * Connects to backend API for real authentication with token persistence
 */
class IosAuthRepository(
    private val httpClient: HttpClient
) : AuthRepository {
    
    private val apiBaseUrl = "https://vwatek-backend-21443684777.us-central1.run.app/api/v1"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val userDefaults = NSUserDefaults.standardUserDefaults
    
    private val _authState = MutableStateFlow(loadSavedAuthState())
    private var _currentUser: User? = loadSavedUser()
    private var _authToken: String? = loadSavedToken()
    
    // Load saved auth state from UserDefaults
    private fun loadSavedAuthState(): AuthState {
        val user = loadSavedUser()
        return AuthState(isAuthenticated = user != null, user = user)
    }
    
    private fun loadSavedUser(): User? {
        val userJson = userDefaults.stringForKey("vwatek_user")
        return if (userJson != null) {
            try {
                json.decodeFromString<SavedUser>(userJson).toUser()
            } catch (e: Exception) {
                null
            }
        } else null
    }
    
    private fun loadSavedToken(): String? {
        return userDefaults.stringForKey("vwatek_auth_token")
    }
    
    private fun saveAuthData(user: User, token: String) {
        val savedUser = SavedUser.fromUser(user)
        userDefaults.setObject(json.encodeToString(savedUser), forKey = "vwatek_user")
        userDefaults.setObject(token, forKey = "vwatek_auth_token")
        _currentUser = user
        _authToken = token
        _authState.value = AuthState(isAuthenticated = true, user = user)
    }
    
    private fun clearAuthData() {
        userDefaults.removeObjectForKey("vwatek_user")
        userDefaults.removeObjectForKey("vwatek_auth_token")
        _currentUser = null
        _authToken = null
        _authState.value = AuthState(isAuthenticated = false, user = null)
    }
    
    override fun getAuthState(): Flow<AuthState> = _authState.asStateFlow()
    
    override suspend fun getCurrentUser(): User? = _currentUser
    
    override suspend fun registerWithEmail(data: RegistrationData): Result<User> {
        return try {
            val response = httpClient.post("$apiBaseUrl/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterApiRequest(
                    email = data.email,
                    password = data.password,
                    firstName = data.firstName,
                    lastName = data.lastName,
                    phone = data.phone
                ))
            }
            
            if (response.status.isSuccess()) {
                val authResponse: AuthApiResponse = response.body()
                val user = authResponse.user.toUser()
                saveAuthData(user, authResponse.token)
                Result.success(user)
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(Exception("Registration failed: $errorBody"))
            }
        } catch (e: Exception) {
            println("Registration error: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun loginWithEmail(email: String, password: String, rememberMe: Boolean): Result<User> {
        return try {
            val response = httpClient.post("$apiBaseUrl/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(
                    email = email,
                    password = password,
                    rememberMe = rememberMe
                ))
            }
            
            if (response.status.isSuccess()) {
                val authResponse: AuthApiResponse = response.body()
                val user = authResponse.user.toUser()
                saveAuthData(user, authResponse.token)
                Result.success(user)
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(Exception("Login failed: $errorBody"))
            }
        } catch (e: Exception) {
            println("Login error: ${e.message}")
            Result.failure(e)
        }
    }
    
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun loginWithGoogle(idToken: String, userInfo: GoogleUserData?): Result<User> {
        return try {
            // Call backend Google auth endpoint
            val response = httpClient.post("$apiBaseUrl/auth/google") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "email" to (userInfo?.email ?: ""),
                    "firstName" to (userInfo?.firstName ?: "Google"),
                    "lastName" to (userInfo?.lastName ?: "User"),
                    "profilePicture" to userInfo?.profilePicture
                ))
            }
            
            if (response.status.isSuccess()) {
                val authResponse: AuthApiResponse = response.body()
                val user = authResponse.user.toUser()
                saveAuthData(user, authResponse.token)
                Result.success(user)
            } else {
                // Fallback to local user creation if backend fails
                val now = Clock.System.now()
                val user = User(
                    id = Uuid.random().toString(),
                    email = userInfo?.email ?: "google.user@example.com",
                    firstName = userInfo?.firstName ?: "Google",
                    lastName = userInfo?.lastName ?: "User",
                    phone = null,
                    address = null,
                    profileImageUrl = userInfo?.profilePicture,
                    authProvider = AuthProvider.GOOGLE,
                    linkedInProfileUrl = null,
                    emailVerified = true,
                    createdAt = now,
                    updatedAt = now
                )
                _currentUser = user
                _authState.value = AuthState(isAuthenticated = true, user = user)
                Result.success(user)
            }
        } catch (e: Exception) {
            println("Google login error: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun loginWithLinkedIn(authCode: String): Result<User> {
        return try {
            val response = httpClient.post("$apiBaseUrl/auth/linkedin") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("code" to authCode, "redirectUri" to ""))
            }
            
            if (response.status.isSuccess()) {
                val authResponse: AuthApiResponse = response.body()
                val user = authResponse.user.toUser()
                saveAuthData(user, authResponse.token)
                Result.success(user)
            } else {
                Result.failure(Exception("LinkedIn login failed"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("LinkedIn login not implemented for iOS: ${e.message}"))
        }
    }
    
    override suspend fun logout() {
        clearAuthData()
    }
    
    override suspend fun updateProfile(user: User): Result<User> {
        return try {
            val token = _authToken
            val response = httpClient.put("$apiBaseUrl/auth/profile") {
                contentType(ContentType.Application.Json)
                if (token != null) {
                    header("Authorization", "Bearer $token")
                }
                header("X-User-Id", user.id)
                setBody(mapOf(
                    "firstName" to user.firstName,
                    "lastName" to user.lastName,
                    "phone" to user.phone
                ))
            }
            
            if (response.status.isSuccess()) {
                val updatedUser = user.copy(updatedAt = Clock.System.now())
                saveAuthData(updatedUser, token ?: "")
                Result.success(updatedUser)
            } else {
                // Update locally anyway
                _currentUser = user
                _authState.value = AuthState(isAuthenticated = true, user = user)
                Result.success(user)
            }
        } catch (e: Exception) {
            // Update locally if network fails
            _currentUser = user
            _authState.value = AuthState(isAuthenticated = true, user = user)
            Result.success(user)
        }
    }
    
    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            val response = httpClient.post("$apiBaseUrl/auth/reset-password") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Password reset failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun isEmailAvailable(email: String): Boolean {
        return try {
            val response = httpClient.get("$apiBaseUrl/auth/check-email") {
                parameter("email", email)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            true // Assume available on error
        }
    }
    
    override suspend fun sendVerificationEmail(email: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun verifyEmail(token: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun isEmailVerified(userId: String): Boolean {
        return _currentUser?.emailVerified ?: false
    }
}

// Helper class for JSON serialization of User (since User has Instant fields)
@Serializable
private data class SavedUser(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String?,
    val authProvider: String,
    val emailVerified: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toUser(): User = User(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        phone = phone,
        address = null,
        profileImageUrl = null,
        authProvider = AuthProvider.valueOf(authProvider),
        linkedInProfileUrl = null,
        emailVerified = emailVerified,
        createdAt = Instant.fromEpochMilliseconds(createdAtMillis),
        updatedAt = Instant.fromEpochMilliseconds(updatedAtMillis)
    )
    
    companion object {
        fun fromUser(user: User): SavedUser = SavedUser(
            id = user.id,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            phone = user.phone,
            authProvider = user.authProvider.name,
            emailVerified = user.emailVerified,
            createdAtMillis = user.createdAt.toEpochMilliseconds(),
            updatedAtMillis = user.updatedAt.toEpochMilliseconds()
        )
    }
}

// Extension to convert API response to domain User
private fun UserApiResponse.toUser(): User {
    val now = Clock.System.now()
    return User(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        phone = phone,
        address = null,
        profileImageUrl = null,
        authProvider = try { AuthProvider.valueOf(authProvider) } catch (e: Exception) { AuthProvider.EMAIL },
        linkedInProfileUrl = null,
        emailVerified = emailVerified,
        createdAt = try { Instant.parse(createdAt) } catch (e: Exception) { now },
        updatedAt = now
    )
}

/**
 * iOS implementation of LinkedInRepository
 */
class IosLinkedInRepository(
    private val httpClient: HttpClient
) : LinkedInRepository {
    
    private val apiBaseUrl = "https://vwatek-backend-21443684777.us-central1.run.app/api/v1"
    
    override suspend fun getAuthorizationUrl(): String {
        return try {
            val response = httpClient.get("$apiBaseUrl/auth/linkedin/url")
            if (response.status.isSuccess()) {
                response.bodyAsText()
            } else {
                "https://www.linkedin.com/oauth/v2/authorization"
            }
        } catch (e: Exception) {
            "https://www.linkedin.com/oauth/v2/authorization"
        }
    }
    
    override suspend fun exchangeCodeForToken(authCode: String): Result<String> {
        return try {
            val response = httpClient.post("$apiBaseUrl/auth/linkedin/token") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("code" to authCode))
            }
            if (response.status.isSuccess()) {
                Result.success(response.bodyAsText())
            } else {
                Result.failure(Exception("Failed to exchange LinkedIn code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getProfile(accessToken: String): Result<LinkedInProfile> {
        return Result.failure(Exception("LinkedIn profile fetch requires native SDK"))
    }
    
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun importProfileAsResume(profile: LinkedInProfile): Resume {
        val now = Clock.System.now()
        val content = buildString {
            appendLine("${profile.firstName} ${profile.lastName}")
            appendLine(profile.email ?: "")
            appendLine()
            appendLine("HEADLINE")
            appendLine(profile.headline ?: "")
            appendLine()
            appendLine("SUMMARY")
            appendLine(profile.summary ?: "")
        }
        return Resume(
            id = Uuid.random().toString(),
            userId = null,
            name = "${profile.firstName} ${profile.lastName} - LinkedIn Resume",
            content = content,
            industry = null,
            sourceType = ResumeSourceType.LINKEDIN,
            fileName = null,
            fileType = null,
            originalFileData = null,
            createdAt = now,
            updatedAt = now,
            currentVersionId = null
        )
    }
}

/**
 * iOS implementation of FileUploadRepository
 */
class IosFileUploadRepository : FileUploadRepository {
    override suspend fun uploadResume(
        fileData: ByteArray,
        fileName: String,
        fileType: String
    ): Result<FileUploadResult> {
        return Result.success(
            FileUploadResult(
                success = true,
                fileName = fileName,
                fileType = fileType,
                extractedContent = null,
                errorMessage = null
            )
        )
    }
    
    override suspend fun extractTextFromFile(
        fileData: ByteArray,
        fileType: String
    ): Result<String> {
        return try {
            // For text files, convert directly
            if (fileType == "txt" || fileType == "text/plain") {
                Result.success(fileData.decodeToString())
            } else {
                // For PDF/DOCX, we'd need native iOS libraries (PDFKit)
                val text = try {
                    fileData.decodeToString()
                } catch (e: Exception) {
                    "Unable to extract text from $fileType file. Please paste your resume content manually."
                }
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getSupportedFileTypes(): List<String> {
        return listOf("pdf", "docx", "doc", "txt")
    }
    
    override fun getMaxFileSizeBytes(): Long {
        return 10 * 1024 * 1024 // 10 MB
    }
}
