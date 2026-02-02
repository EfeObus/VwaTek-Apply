package com.vwatek.apply.domain.repository

import com.vwatek.apply.domain.model.Resume
import com.vwatek.apply.domain.model.ResumeVersion
import com.vwatek.apply.domain.model.ResumeAnalysis
import com.vwatek.apply.domain.model.CoverLetter
import com.vwatek.apply.domain.model.InterviewSession
import com.vwatek.apply.domain.model.InterviewQuestion
import com.vwatek.apply.domain.model.User
import com.vwatek.apply.domain.model.AuthState
import com.vwatek.apply.domain.model.RegistrationData
import com.vwatek.apply.domain.model.LinkedInProfile
import com.vwatek.apply.domain.model.FileUploadResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getAuthState(): Flow<AuthState>
    suspend fun getCurrentUser(): User?
    suspend fun registerWithEmail(data: RegistrationData): Result<User>
    suspend fun loginWithEmail(email: String, password: String, rememberMe: Boolean = true): Result<User>
    suspend fun loginWithGoogle(idToken: String, userInfo: GoogleUserData? = null): Result<User>
    suspend fun loginWithLinkedIn(authCode: String): Result<User>
    suspend fun logout()
    suspend fun updateProfile(user: User): Result<User>
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun isEmailAvailable(email: String): Boolean
    
    // Email verification methods (stub for future implementation)
    suspend fun sendVerificationEmail(email: String): Result<Unit>
    suspend fun verifyEmail(token: String): Result<Unit>
    suspend fun isEmailVerified(userId: String): Boolean
}

/**
 * Sealed class for authentication errors with user-friendly messages
 */
sealed class AuthError(override val message: String) : Exception(message) {
    // Login errors
    data object InvalidCredentials : AuthError("The email or password you entered is incorrect. Please try again.")
    data object AccountNotFound : AuthError("No account found with this email address. Please register first.")
    data object AccountLocked : AuthError("Your account has been temporarily locked due to too many failed login attempts. Please try again later.")
    
    // Registration errors
    data object EmailAlreadyExists : AuthError("An account with this email already exists. Please sign in or use a different email.")
    data object WeakPassword : AuthError("Password is too weak. Please use at least 8 characters with a mix of letters, numbers, and symbols.")
    data object InvalidEmail : AuthError("Please enter a valid email address.")
    
    // OAuth errors
    data object GoogleSignInCancelled : AuthError("Google sign-in was cancelled. Please try again.")
    data object GoogleSignInFailed : AuthError("Unable to sign in with Google. Please check your internet connection and try again.")
    data object LinkedInSignInCancelled : AuthError("LinkedIn sign-in was cancelled. Please try again.")
    data object LinkedInSignInFailed : AuthError("Unable to sign in with LinkedIn. Please check your internet connection and try again.")
    data object OAuthTokenExpired : AuthError("Your session has expired. Please sign in again.")
    
    // Session errors
    data object SessionExpired : AuthError("Your session has expired. Please sign in again.")
    data object NotAuthenticated : AuthError("Please sign in to continue.")
    
    // Email verification errors
    data object EmailNotVerified : AuthError("Please verify your email address to continue.")
    data object VerificationEmailFailed : AuthError("Unable to send verification email. Please try again later.")
    data object InvalidVerificationToken : AuthError("The verification link is invalid or has expired. Please request a new one.")
    
    // Network errors
    data object NetworkError : AuthError("Unable to connect. Please check your internet connection and try again.")
    data object ServerError : AuthError("Something went wrong on our end. Please try again later.")
    
    // Password reset errors
    data object PasswordResetFailed : AuthError("Unable to send password reset email. Please verify your email address and try again.")
    
    // Generic error
    data class Unknown(override val message: String) : AuthError(message)
}

/**
 * Data class to pass Google user info for account creation
 */
data class GoogleUserData(
    val email: String,
    val firstName: String,
    val lastName: String,
    val profilePicture: String? = null
)

interface LinkedInRepository {
    suspend fun getAuthorizationUrl(): String
    suspend fun exchangeCodeForToken(authCode: String): Result<String>
    suspend fun getProfile(accessToken: String): Result<LinkedInProfile>
    suspend fun importProfileAsResume(profile: LinkedInProfile): Resume
}

interface FileUploadRepository {
    suspend fun uploadResume(
        fileData: ByteArray,
        fileName: String,
        fileType: String
    ): Result<FileUploadResult>
    suspend fun extractTextFromFile(
        fileData: ByteArray,
        fileType: String
    ): Result<String>
    fun getSupportedFileTypes(): List<String>
    fun getMaxFileSizeBytes(): Long
}

interface ResumeRepository {
    fun getAllResumes(): Flow<List<Resume>>
    suspend fun getResumeById(id: String): Resume?
    suspend fun getResumesByUserId(userId: String): List<Resume>
    suspend fun insertResume(resume: Resume)
    suspend fun updateResume(resume: Resume)
    suspend fun deleteResume(id: String)
    
    // Version control methods
    fun getVersionsByResumeId(resumeId: String): Flow<List<ResumeVersion>>
    suspend fun getVersionById(id: String): ResumeVersion?
    suspend fun insertVersion(version: ResumeVersion)
    suspend fun deleteVersion(id: String)
    suspend fun deleteVersionsByResumeId(resumeId: String)
}

interface AnalysisRepository {
    fun getAnalysesByResumeId(resumeId: String): Flow<List<ResumeAnalysis>>
    suspend fun insertAnalysis(analysis: ResumeAnalysis)
    suspend fun deleteAnalysis(id: String)
}

interface CoverLetterRepository {
    fun getAllCoverLetters(): Flow<List<CoverLetter>>
    suspend fun getCoverLetterById(id: String): CoverLetter?
    suspend fun insertCoverLetter(coverLetter: CoverLetter)
    suspend fun updateCoverLetter(id: String, content: String)
    suspend fun deleteCoverLetter(id: String)
}

interface InterviewRepository {
    fun getAllSessions(): Flow<List<InterviewSession>>
    suspend fun getSessionById(id: String): InterviewSession?
    suspend fun insertSession(session: InterviewSession)
    suspend fun updateSessionStatus(id: String, status: String, completedAt: Long?)
    suspend fun deleteSession(id: String)
    fun getQuestionsBySessionId(sessionId: String): Flow<List<InterviewQuestion>>
    suspend fun insertQuestion(question: InterviewQuestion)
    suspend fun updateQuestion(id: String, userAnswer: String?, aiFeedback: String?)
}

interface SettingsRepository {
    suspend fun getSetting(key: String): String?
    suspend fun setSetting(key: String, value: String)
    suspend fun deleteSetting(key: String)
}
