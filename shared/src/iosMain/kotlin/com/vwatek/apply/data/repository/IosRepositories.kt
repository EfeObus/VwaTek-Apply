package com.vwatek.apply.data.repository

import com.vwatek.apply.domain.model.*
import com.vwatek.apply.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * iOS implementation of AuthRepository
 * Uses in-memory storage for simplicity
 */
class IosAuthRepository : AuthRepository {
    
    private val _authState = MutableStateFlow(AuthState())
    private var _currentUser: User? = null
    
    override fun getAuthState(): Flow<AuthState> = _authState.asStateFlow()
    
    override suspend fun getCurrentUser(): User? = _currentUser
    
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun registerWithEmail(data: RegistrationData): Result<User> {
        return try {
            val now = Clock.System.now()
            val user = User(
                id = Uuid.random().toString(),
                email = data.email,
                firstName = data.firstName,
                lastName = data.lastName,
                phone = data.phone,
                address = data.address,
                profileImageUrl = null,
                authProvider = AuthProvider.EMAIL,
                linkedInProfileUrl = null,
                emailVerified = false,
                createdAt = now,
                updatedAt = now
            )
            _currentUser = user
            _authState.value = AuthState(isAuthenticated = true, user = user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun loginWithEmail(email: String, password: String, rememberMe: Boolean): Result<User> {
        return try {
            // For demo purposes, create a user on login
            val now = Clock.System.now()
            val user = User(
                id = "demo-user-id",
                email = email,
                firstName = "Demo",
                lastName = "User",
                phone = null,
                address = null,
                profileImageUrl = null,
                authProvider = AuthProvider.EMAIL,
                linkedInProfileUrl = null,
                emailVerified = true,
                createdAt = now,
                updatedAt = now
            )
            _currentUser = user
            _authState.value = AuthState(isAuthenticated = true, user = user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun loginWithGoogle(idToken: String, userInfo: GoogleUserData?): Result<User> {
        return try {
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun loginWithLinkedIn(authCode: String): Result<User> {
        return Result.failure(Exception("LinkedIn login not implemented for iOS"))
    }
    
    override suspend fun logout() {
        _currentUser = null
        _authState.value = AuthState(isAuthenticated = false, user = null)
    }
    
    override suspend fun updateProfile(user: User): Result<User> {
        return try {
            _currentUser = user
            _authState.value = AuthState(isAuthenticated = true, user = user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun resetPassword(email: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun isEmailAvailable(email: String): Boolean {
        return true
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

/**
 * iOS implementation of LinkedInRepository (stub)
 */
class IosLinkedInRepository : LinkedInRepository {
    override suspend fun getAuthorizationUrl(): String {
        return "https://www.linkedin.com/oauth/v2/authorization"
    }
    
    override suspend fun exchangeCodeForToken(authCode: String): Result<String> {
        return Result.failure(Exception("LinkedIn auth not implemented for iOS"))
    }
    
    override suspend fun getProfile(accessToken: String): Result<LinkedInProfile> {
        return Result.failure(Exception("LinkedIn profile not implemented for iOS"))
    }
    
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun importProfileAsResume(profile: LinkedInProfile): Resume {
        val now = Clock.System.now()
        return Resume(
            id = Uuid.random().toString(),
            userId = null,
            name = "LinkedIn Resume",
            content = "Imported from LinkedIn",
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
                // For PDF/DOCX, we'd need native iOS libraries
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
