package com.vwatek.apply.data.repository

import com.vwatek.apply.db.VwaTekDatabase
import com.vwatek.apply.domain.model.AuthState
import com.vwatek.apply.domain.model.User
import com.vwatek.apply.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * iOS implementation of AuthRepository
 * Uses local storage via SQLDelight for user data
 */
class IosAuthRepository(
    private val database: VwaTekDatabase
) : AuthRepository {
    
    private val queries = database.vwaTekDatabaseQueries
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    private var _currentUser: User? = null
    
    init {
        // Check for saved auth state
        checkSavedAuth()
    }
    
    private fun checkSavedAuth() {
        try {
            val savedUser = queries.selectCurrentUser().executeAsOneOrNull()
            if (savedUser != null) {
                _currentUser = User(
                    id = savedUser.id,
                    email = savedUser.email,
                    firstName = savedUser.firstName ?: "",
                    lastName = savedUser.lastName ?: "",
                    phone = savedUser.phone,
                    profileImageUrl = savedUser.profileImageUrl,
                    emailVerified = savedUser.emailVerified == 1L,
                    provider = savedUser.provider ?: "email",
                    createdAt = Clock.System.now()
                )
                _authState.value = AuthState.Authenticated(_currentUser!!)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    override fun getAuthState(): Flow<AuthState> = _authState.asStateFlow()
    
    override suspend fun getCurrentUser(): User? = _currentUser
    
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun registerWithEmail(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<User> {
        return try {
            val userId = Uuid.random().toString()
            val user = User(
                id = userId,
                email = email,
                firstName = firstName,
                lastName = lastName,
                phone = null,
                profileImageUrl = null,
                emailVerified = false,
                provider = "email",
                createdAt = Clock.System.now()
            )
            
            // Save to local database
            queries.insertCurrentUser(
                id = user.id,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                phone = user.phone,
                profileImageUrl = user.profileImageUrl,
                emailVerified = if (user.emailVerified) 1L else 0L,
                provider = user.provider
            )
            
            _currentUser = user
            _authState.value = AuthState.Authenticated(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun loginWithEmail(email: String, password: String): Result<User> {
        return try {
            // For now, check if user exists in local database
            val savedUser = queries.selectUserByEmail(email).executeAsOneOrNull()
            
            if (savedUser != null) {
                val user = User(
                    id = savedUser.id,
                    email = savedUser.email,
                    firstName = savedUser.firstName ?: "",
                    lastName = savedUser.lastName ?: "",
                    phone = savedUser.phone,
                    profileImageUrl = savedUser.profileImageUrl,
                    emailVerified = savedUser.emailVerified == 1L,
                    provider = savedUser.provider ?: "email",
                    createdAt = Clock.System.now()
                )
                _currentUser = user
                _authState.value = AuthState.Authenticated(user)
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun loginWithGoogle(idToken: String): Result<User> {
        // iOS Google Sign-In implementation
        val user = User(
            id = Uuid.random().toString(),
            email = "google.user@example.com",
            firstName = "Google",
            lastName = "User",
            phone = null,
            profileImageUrl = null,
            emailVerified = true,
            provider = "google",
            createdAt = Clock.System.now()
        )
        _currentUser = user
        _authState.value = AuthState.Authenticated(user)
        return Result.success(user)
    }
    
    override suspend fun loginWithLinkedIn(authCode: String): Result<User> {
        return Result.failure(Exception("LinkedIn login not implemented for iOS"))
    }
    
    override suspend fun logout() {
        _currentUser = null
        _authState.value = AuthState.Unauthenticated
        try {
            queries.deleteCurrentUser()
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    override suspend fun resetPassword(email: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun updateProfile(user: User): Result<User> {
        return try {
            queries.updateCurrentUser(
                firstName = user.firstName,
                lastName = user.lastName,
                phone = user.phone,
                profileImageUrl = user.profileImageUrl,
                id = user.id
            )
            _currentUser = user
            _authState.value = AuthState.Authenticated(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun sendVerificationEmail(): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun isEmailAvailable(email: String): Boolean {
        return queries.selectUserByEmail(email).executeAsOneOrNull() == null
    }
}

/**
 * iOS implementation of LinkedInRepository (stub)
 */
class IosLinkedInRepository : com.vwatek.apply.domain.repository.LinkedInRepository {
    override fun getAuthorizationUrl(): String {
        return "https://www.linkedin.com/oauth/v2/authorization"
    }
    
    override suspend fun exchangeCodeForToken(authCode: String): Result<String> {
        return Result.failure(Exception("LinkedIn auth not implemented for iOS"))
    }
    
    override suspend fun getProfile(accessToken: String): Result<com.vwatek.apply.domain.model.LinkedInProfile> {
        return Result.failure(Exception("LinkedIn profile not implemented for iOS"))
    }
}

/**
 * iOS implementation of FileUploadRepository
 */
class IosFileUploadRepository : com.vwatek.apply.domain.repository.FileUploadRepository {
    override suspend fun uploadResume(
        fileData: ByteArray,
        fileName: String,
        fileType: String
    ): Result<String> {
        // Return a mock URL for now
        return Result.success("file://${fileName}")
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
                // For PDF/DOCX, we'd need native iOS libraries
                // For now, try to extract text or return placeholder
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
        return 10 * 1024 * 1024 // 10MB
    }
}
