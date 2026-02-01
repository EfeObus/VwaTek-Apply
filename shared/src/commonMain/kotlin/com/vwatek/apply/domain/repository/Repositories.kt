package com.vwatek.apply.domain.repository

import com.vwatek.apply.domain.model.Resume
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
    suspend fun loginWithEmail(email: String, password: String): Result<User>
    suspend fun loginWithGoogle(idToken: String): Result<User>
    suspend fun loginWithLinkedIn(authCode: String): Result<User>
    suspend fun logout()
    suspend fun updateProfile(user: User): Result<User>
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun isEmailAvailable(email: String): Boolean
}

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
