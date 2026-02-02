package com.vwatek.apply.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

// User Authentication Models
@Serializable
data class User(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String? = null,
    val address: Address? = null,
    val profileImageUrl: String? = null,
    val authProvider: AuthProvider,
    val linkedInProfileUrl: String? = null,
    val emailVerified: Boolean = false,  // Email verification status
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class Address(
    val street: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zipCode: String? = null,
    val country: String? = null
)

@Serializable
enum class AuthProvider {
    EMAIL,
    GOOGLE,
    LINKEDIN
}

@Serializable
data class AuthState(
    val isAuthenticated: Boolean = false,
    val user: User? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null
)

@Serializable
data class LoginCredentials(
    val email: String,
    val password: String
)

@Serializable
data class RegistrationData(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val phone: String? = null,
    val address: Address? = null
)

// Resume Models
@Serializable
data class Resume(
    val id: String,
    val userId: String? = null,
    val name: String,
    val content: String,
    val industry: String? = null,
    val sourceType: ResumeSourceType = ResumeSourceType.MANUAL,
    val fileName: String? = null,
    val fileType: String? = null,
    val originalFileData: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val currentVersionId: String? = null
)

@Serializable
enum class ResumeSourceType {
    MANUAL,
    UPLOADED,
    LINKEDIN
}

@Serializable
data class ResumeVersion(
    val id: String,
    val resumeId: String,
    val versionNumber: Int,
    val content: String,
    val changeDescription: String,
    val createdAt: Instant
) {
    /** Formatted date string for UI display (avoids Instant accessibility issues in webApp) */
    val createdAtFormatted: String
        get() = createdAt.toString().take(19).replace("T", " ")
}

@Serializable
data class ResumeAnalysis(
    val id: String,
    val resumeId: String,
    val jobDescription: String,
    val matchScore: Int,
    val missingKeywords: List<String>,
    val recommendations: List<String>,
    val createdAt: Instant
)

@Serializable
data class CoverLetter(
    val id: String,
    val resumeId: String? = null,
    val jobTitle: String,
    val companyName: String,
    val content: String,
    val tone: CoverLetterTone,
    val createdAt: Instant
)

@Serializable
enum class CoverLetterTone {
    PROFESSIONAL,
    ENTHUSIASTIC,
    FORMAL,
    CREATIVE
}

@Serializable
data class InterviewSession(
    val id: String,
    val resumeId: String? = null,
    val jobTitle: String,
    val jobDescription: String,
    val status: InterviewStatus,
    val questions: List<InterviewQuestion> = emptyList(),
    val createdAt: Instant,
    val completedAt: Instant? = null
)

@Serializable
enum class InterviewStatus {
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

@Serializable
data class InterviewQuestion(
    val id: String,
    val sessionId: String,
    val question: String,
    val userAnswer: String? = null,
    val aiFeedback: String? = null,
    val questionOrder: Int,
    val createdAt: Instant
)

// LinkedIn Import Models
@Serializable
data class LinkedInProfile(
    val id: String,
    val firstName: String,
    val lastName: String,
    val headline: String? = null,
    val summary: String? = null,
    val email: String? = null,
    val profileUrl: String? = null,
    val profileImageUrl: String? = null,
    val positions: List<LinkedInPosition> = emptyList(),
    val education: List<LinkedInEducation> = emptyList(),
    val skills: List<String> = emptyList()
)

@Serializable
data class LinkedInPosition(
    val title: String,
    val companyName: String,
    val location: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val description: String? = null,
    val isCurrent: Boolean = false
)

@Serializable
data class LinkedInEducation(
    val schoolName: String,
    val degree: String? = null,
    val fieldOfStudy: String? = null,
    val startYear: String? = null,
    val endYear: String? = null
)

// File Upload Models
@Serializable
data class FileUploadResult(
    val success: Boolean,
    val fileName: String? = null,
    val fileType: String? = null,
    val extractedContent: String? = null,
    val errorMessage: String? = null
)

@Serializable
enum class SupportedFileType {
    PDF,
    DOCX,
    DOC,
    TXT
}

// ATS Analysis Models
@Serializable
data class ATSAnalysis(
    val id: String,
    val resumeId: String,
    val overallScore: Int,
    val formattingScore: Int,
    val keywordScore: Int,
    val structureScore: Int,
    val readabilityScore: Int,
    val formattingIssues: List<ATSIssue>,
    val structureIssues: List<ATSIssue>,
    val keywordDensity: Map<String, Int>,
    val recommendations: List<ATSRecommendation>,
    val impactBullets: List<ImpactBullet>,
    val grammarIssues: List<GrammarIssue>,
    val createdAt: Instant
)

@Serializable
data class ATSIssue(
    val severity: IssueSeverity,
    val category: String,
    val description: String,
    val suggestion: String
)

@Serializable
enum class IssueSeverity {
    HIGH,
    MEDIUM,
    LOW
}

@Serializable
data class ATSRecommendation(
    val priority: Int,
    val category: String,
    val title: String,
    val description: String,
    val impact: String
)

@Serializable
data class ImpactBullet(
    val original: String,
    val improved: String,
    val xyzFormat: XYZFormat?
)

@Serializable
data class XYZFormat(
    val accomplished: String,
    val measuredBy: String,
    val byDoing: String
)

@Serializable
data class GrammarIssue(
    val original: String,
    val corrected: String,
    val explanation: String,
    val type: GrammarIssueType
)

@Serializable
enum class GrammarIssueType {
    GRAMMAR,
    SPELLING,
    TONE,
    CLARITY,
    REDUNDANCY
}
