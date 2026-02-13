package com.vwatek.apply.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * LinkedIn Profile Optimizer Models for VwaTek Apply
 * Provides AI-powered LinkedIn profile analysis and optimization
 */

/**
 * LinkedIn profile data (imported or manually entered)
 */
@Serializable
data class LinkedInProfile(
    val id: String,
    val userId: String,
    val linkedInUrl: String? = null,
    val headline: String? = null,
    val summary: String? = null,
    val currentPosition: LinkedInPosition? = null,
    val positions: List<LinkedInPosition> = emptyList(),
    val education: List<LinkedInEducation> = emptyList(),
    val skills: List<String> = emptyList(),
    val endorsements: Map<String, Int> = emptyMap(),     // Skill -> endorsement count
    val recommendations: Int = 0,
    val connections: Int = 0,
    val profilePhotoUrl: String? = null,
    val bannerPhotoUrl: String? = null,
    val industry: String? = null,
    val location: String? = null,
    val importedAt: Instant? = null,
    val lastAnalyzedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * LinkedIn work position
 */
@Serializable
data class LinkedInPosition(
    val title: String,
    val company: String,
    val companyLinkedInUrl: String? = null,
    val location: String? = null,
    val description: String? = null,
    val startDate: String? = null,       // "YYYY-MM" format
    val endDate: String? = null,         // null if current
    val isCurrent: Boolean = false
)

/**
 * LinkedIn education entry
 */
@Serializable
data class LinkedInEducation(
    val school: String,
    val degree: String? = null,
    val fieldOfStudy: String? = null,
    val startYear: Int? = null,
    val endYear: Int? = null,
    val activities: String? = null,
    val description: String? = null
)

/**
 * LinkedIn profile analysis result
 */
@Serializable
data class LinkedInAnalysis(
    val id: String,
    val profileId: String,
    val userId: String,
    val overallScore: Int,               // 0-100
    val sectionScores: LinkedInSectionScores,
    val strengths: List<String>,
    val improvements: List<LinkedInImprovement>,
    val keywordAnalysis: KeywordAnalysis,
    val industryBenchmark: IndustryBenchmark,
    val recommendations: List<LinkedInRecommendation>,
    val optimizedContent: OptimizedLinkedInContent? = null,
    val analyzedAt: Instant
)

/**
 * Scores for each LinkedIn profile section
 */
@Serializable
data class LinkedInSectionScores(
    val headline: Int,           // 0-100
    val summary: Int,
    val experience: Int,
    val education: Int,
    val skills: Int,
    val completeness: Int
)

/**
 * Suggested improvement for LinkedIn profile
 */
@Serializable
data class LinkedInImprovement(
    val section: LinkedInSection,
    val priority: ImprovementPriority,
    val issue: String,
    val suggestion: String,
    val expectedImpact: String
)

/**
 * LinkedIn profile sections
 */
@Serializable
enum class LinkedInSection {
    HEADLINE,
    SUMMARY,
    EXPERIENCE,
    EDUCATION,
    SKILLS,
    PHOTO,
    BANNER,
    CONTACT_INFO,
    FEATURED
}

/**
 * Improvement priority level
 */
@Serializable
enum class ImprovementPriority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Keyword analysis for LinkedIn SEO
 */
@Serializable
data class KeywordAnalysis(
    val foundKeywords: List<String>,
    val missingKeywords: List<String>,
    val industryKeywords: List<String>,
    val roleKeywords: List<String>,
    val keywordDensity: Double,
    val atsCompatibility: Int           // 0-100 score
)

/**
 * Comparison with industry benchmarks
 */
@Serializable
data class IndustryBenchmark(
    val industry: String,
    val averageScore: Int,
    val userRanking: Int,               // Percentile ranking
    val topPerformerTraits: List<String>,
    val commonSkills: List<String>
)

/**
 * Actionable recommendation
 */
@Serializable
data class LinkedInRecommendation(
    val id: String,
    val category: RecommendationCategory,
    val title: String,
    val description: String,
    val actionItems: List<String>,
    val estimatedTime: String,          // "5 minutes", "10-15 minutes"
    val impactLevel: ImprovementPriority
)

/**
 * Recommendation categories
 */
@Serializable
enum class RecommendationCategory {
    PROFILE_COMPLETENESS,
    KEYWORD_OPTIMIZATION,
    ENGAGEMENT,
    NETWORKING,
    CONTENT_QUALITY,
    VISUAL_BRANDING
}

/**
 * AI-generated optimized content suggestions
 */
@Serializable
data class OptimizedLinkedInContent(
    val headline: String? = null,
    val headlineAlternatives: List<String> = emptyList(),
    val summary: String? = null,
    val summaryAlternatives: List<String> = emptyList(),
    val experienceDescriptions: Map<String, String> = emptyMap(),    // Position title -> optimized description
    val skillSuggestions: List<String> = emptyList()
)

/**
 * Request to import LinkedIn profile
 */
@Serializable
data class ImportLinkedInRequest(
    val linkedInUrl: String
)

/**
 * Request to manually enter LinkedIn profile data
 */
@Serializable
data class ManualLinkedInProfileRequest(
    val headline: String? = null,
    val summary: String? = null,
    val currentPosition: LinkedInPosition? = null,
    val positions: List<LinkedInPosition> = emptyList(),
    val education: List<LinkedInEducation> = emptyList(),
    val skills: List<String> = emptyList(),
    val industry: String? = null,
    val location: String? = null
)

/**
 * Request to analyze LinkedIn profile
 */
@Serializable
data class AnalyzeLinkedInRequest(
    val targetRole: String? = null,        // Target job role for optimization
    val targetIndustry: String? = null,    // Target industry
    val includeContentSuggestions: Boolean = true
)

/**
 * LinkedIn profile analysis history
 */
@Serializable
data class LinkedInAnalysisHistory(
    val id: String,
    val userId: String,
    val profileId: String,
    val analysis: LinkedInAnalysis,
    val appliedRecommendations: List<String> = emptyList(),  // IDs of applied recommendations
    val createdAt: Instant
)

/**
 * LinkedIn profile update tracking
 */
@Serializable
data class LinkedInProfileUpdate(
    val profileId: String,
    val section: LinkedInSection,
    val previousContent: String,
    val newContent: String,
    val suggestionId: String? = null,     // If based on a suggestion
    val updatedAt: Instant
)

/**
 * LinkedIn optimization session for coaching
 */
@Serializable
data class LinkedInOptimizationSession(
    val id: String,
    val userId: String,
    val profileId: String,
    val analysis: LinkedInAnalysis,
    val status: OptimizationSessionStatus,
    val appliedChanges: List<LinkedInProfileUpdate>,
    val createdAt: Instant,
    val completedAt: Instant? = null
)

/**
 * Optimization session status
 */
@Serializable
enum class OptimizationSessionStatus {
    IN_PROGRESS,
    COMPLETED,
    ABANDONED
}

/**
 * LinkedIn profile comparison (before/after)
 */
@Serializable
data class LinkedInProfileComparison(
    val beforeScore: Int,
    val afterScore: Int,
    val improvementPercentage: Double,
    val changedSections: List<LinkedInSection>,
    val beforeAnalysis: LinkedInAnalysis,
    val afterAnalysis: LinkedInAnalysis
)
