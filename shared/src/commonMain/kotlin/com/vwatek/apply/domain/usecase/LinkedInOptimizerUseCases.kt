package com.vwatek.apply.domain.usecase

import com.vwatek.apply.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/**
 * Phase 5: LinkedIn Optimizer Use Cases
 * Handles LinkedIn profile analysis and optimization
 */

// Analyze LinkedIn profile
class AnalyzeLinkedInProfileUseCase(
    private val linkedInApiClient: LinkedInApiClient,
    private val subscriptionManager: SubscriptionManager
) {
    suspend operator fun invoke(
        profileUrl: String? = null,
        manualProfile: LinkedInProfile? = null
    ): Result<LinkedInAnalysisResult> {
        // Check premium feature access
        if (!subscriptionManager.canUseFeature(PremiumFeature.LINKEDIN_OPTIMIZER)) {
            return Result.failure(
                FeatureNotAvailableException(
                    feature = PremiumFeature.LINKEDIN_OPTIMIZER,
                    requiredTier = SubscriptionTier.PREMIUM
                )
            )
        }
        
        // Validate input
        if (profileUrl == null && manualProfile == null) {
            return Result.failure(IllegalArgumentException("Either profile URL or manual profile is required"))
        }
        
        return linkedInApiClient.analyzeProfile(profileUrl, manualProfile)
    }
}

// Get optimized content for LinkedIn sections
class GetOptimizedLinkedInContentUseCase(
    private val linkedInApiClient: LinkedInApiClient,
    private val subscriptionManager: SubscriptionManager
) {
    suspend operator fun invoke(
        profileId: String,
        targetRole: String? = null,
        targetIndustry: String? = null,
        focusAreas: List<LinkedInSection> = emptyList()
    ): Result<OptimizedLinkedInContent> {
        // Check premium feature access
        if (!subscriptionManager.canUseFeature(PremiumFeature.LINKEDIN_OPTIMIZER)) {
            return Result.failure(
                FeatureNotAvailableException(
                    feature = PremiumFeature.LINKEDIN_OPTIMIZER,
                    requiredTier = SubscriptionTier.PREMIUM
                )
            )
        }
        
        return linkedInApiClient.getOptimizedContent(
            profileId = profileId,
            targetRole = targetRole,
            targetIndustry = targetIndustry,
            focusAreas = focusAreas
        )
    }
}

// Save optimized LinkedIn profile
class SaveLinkedInProfileUseCase(
    private val linkedInApiClient: LinkedInApiClient
) {
    suspend operator fun invoke(profile: LinkedInProfile): Result<LinkedInProfile> {
        return linkedInApiClient.saveProfile(profile)
    }
}

// Get LinkedIn profile history
class GetLinkedInHistoryUseCase(
    private val linkedInApiClient: LinkedInApiClient
) {
    suspend operator fun invoke(): Result<List<LinkedInAnalysisResult>> {
        return linkedInApiClient.getAnalysisHistory()
    }
}

// Generate LinkedIn headline suggestions
class GenerateLinkedInHeadlineUseCase(
    private val linkedInApiClient: LinkedInApiClient,
    private val subscriptionManager: SubscriptionManager
) {
    suspend operator fun invoke(
        currentHeadline: String,
        targetRole: String,
        skills: List<String>,
        yearsExperience: Int
    ): Result<List<HeadlineSuggestion>> {
        // Check premium feature access
        if (!subscriptionManager.canUseFeature(PremiumFeature.LINKEDIN_OPTIMIZER)) {
            return Result.failure(
                FeatureNotAvailableException(
                    feature = PremiumFeature.LINKEDIN_OPTIMIZER,
                    requiredTier = SubscriptionTier.PREMIUM
                )
            )
        }
        
        return linkedInApiClient.generateHeadlines(
            currentHeadline = currentHeadline,
            targetRole = targetRole,
            skills = skills,
            yearsExperience = yearsExperience
        )
    }
}

// Generate LinkedIn summary/about section
class GenerateLinkedInSummaryUseCase(
    private val linkedInApiClient: LinkedInApiClient,
    private val subscriptionManager: SubscriptionManager
) {
    suspend operator fun invoke(
        currentSummary: String?,
        experience: List<String>,
        skills: List<String>,
        targetRole: String,
        tone: SummaryTone = SummaryTone.PROFESSIONAL
    ): Result<SummarySuggestion> {
        // Check premium feature access
        if (!subscriptionManager.canUseFeature(PremiumFeature.LINKEDIN_OPTIMIZER)) {
            return Result.failure(
                FeatureNotAvailableException(
                    feature = PremiumFeature.LINKEDIN_OPTIMIZER,
                    requiredTier = SubscriptionTier.PREMIUM
                )
            )
        }
        
        return linkedInApiClient.generateSummary(
            currentSummary = currentSummary,
            experience = experience,
            skills = skills,
            targetRole = targetRole,
            tone = tone
        )
    }
}

/**
 * LinkedIn Optimizer Manager - provides reactive state for LinkedIn features
 */
class LinkedInOptimizerManager(
    private val linkedInApiClient: LinkedInApiClient,
    private val subscriptionManager: SubscriptionManager
) {
    private val _profileState = MutableStateFlow<LinkedInProfileState>(LinkedInProfileState.Idle)
    val profileState: StateFlow<LinkedInProfileState> = _profileState
    
    private val _optimizationState = MutableStateFlow<OptimizationState>(OptimizationState.Idle)
    val optimizationState: StateFlow<OptimizationState> = _optimizationState
    
    private val _historyState = MutableStateFlow<LinkedInHistoryState>(LinkedInHistoryState.Loading)
    val historyState: StateFlow<LinkedInHistoryState> = _historyState
    
    suspend fun analyzeProfile(profileUrl: String? = null, manualProfile: LinkedInProfile? = null) {
        _profileState.value = LinkedInProfileState.Analyzing
        
        linkedInApiClient.analyzeProfile(profileUrl, manualProfile)
            .onSuccess { result ->
                _profileState.value = LinkedInProfileState.Analyzed(result)
            }
            .onFailure { error ->
                _profileState.value = LinkedInProfileState.Error(error.message ?: "Failed to analyze profile")
            }
    }
    
    suspend fun getOptimizedContent(
        profileId: String,
        targetRole: String? = null,
        targetIndustry: String? = null
    ) {
        _optimizationState.value = OptimizationState.Generating
        
        linkedInApiClient.getOptimizedContent(profileId, targetRole, targetIndustry, emptyList())
            .onSuccess { content ->
                _optimizationState.value = OptimizationState.Success(content)
            }
            .onFailure { error ->
                _optimizationState.value = OptimizationState.Error(error.message ?: "Failed to generate optimizations")
            }
    }
    
    suspend fun loadHistory() {
        _historyState.value = LinkedInHistoryState.Loading
        
        linkedInApiClient.getAnalysisHistory()
            .onSuccess { history ->
                _historyState.value = LinkedInHistoryState.Success(history)
            }
            .onFailure { error ->
                _historyState.value = LinkedInHistoryState.Error(error.message ?: "Failed to load history")
            }
    }
    
    fun clearProfile() {
        _profileState.value = LinkedInProfileState.Idle
    }
    
    fun clearOptimization() {
        _optimizationState.value = OptimizationState.Idle
    }
}

sealed class LinkedInProfileState {
    data object Idle : LinkedInProfileState()
    data object Analyzing : LinkedInProfileState()
    data class Analyzed(val result: LinkedInAnalysisResult) : LinkedInProfileState()
    data class Error(val message: String) : LinkedInProfileState()
}

sealed class OptimizationState {
    data object Idle : OptimizationState()
    data object Generating : OptimizationState()
    data class Success(val content: OptimizedLinkedInContent) : OptimizationState()
    data class Error(val message: String) : OptimizationState()
}

sealed class LinkedInHistoryState {
    data object Loading : LinkedInHistoryState()
    data class Success(val history: List<LinkedInAnalysisResult>) : LinkedInHistoryState()
    data class Error(val message: String) : LinkedInHistoryState()
}

/**
 * LinkedIn API Client Interface
 */
interface LinkedInApiClient {
    suspend fun analyzeProfile(
        profileUrl: String?,
        manualProfile: LinkedInProfile?
    ): Result<LinkedInAnalysisResult>
    
    suspend fun getOptimizedContent(
        profileId: String,
        targetRole: String?,
        targetIndustry: String?,
        focusAreas: List<LinkedInSection>
    ): Result<OptimizedLinkedInContent>
    
    suspend fun saveProfile(profile: LinkedInProfile): Result<LinkedInProfile>
    
    suspend fun getAnalysisHistory(): Result<List<LinkedInAnalysisResult>>
    
    suspend fun generateHeadlines(
        currentHeadline: String,
        targetRole: String,
        skills: List<String>,
        yearsExperience: Int
    ): Result<List<HeadlineSuggestion>>
    
    suspend fun generateSummary(
        currentSummary: String?,
        experience: List<String>,
        skills: List<String>,
        targetRole: String,
        tone: SummaryTone
    ): Result<SummarySuggestion>
}

/**
 * Supporting data classes
 */
@Serializable
data class LinkedInAnalysisResult(
    val profileId: String,
    val profile: LinkedInProfile,
    val analysis: LinkedInAnalysis,
    val analyzedAt: String
)

@Serializable
data class HeadlineSuggestion(
    val headline: String,
    val score: Int,
    val explanation: String,
    val keywords: List<String>
)

@Serializable
data class SummarySuggestion(
    val summary: String,
    val wordCount: Int,
    val keyThemes: List<String>,
    val callToAction: String?
)

@Serializable
enum class LinkedInSection {
    HEADLINE,
    SUMMARY,
    EXPERIENCE,
    SKILLS,
    EDUCATION,
    CERTIFICATIONS,
    RECOMMENDATIONS
}

@Serializable
enum class SummaryTone {
    PROFESSIONAL,
    CONVERSATIONAL,
    EXECUTIVE,
    CREATIVE,
    TECHNICAL
}
