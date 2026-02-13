package com.vwatek.apply.domain.usecase

import com.vwatek.apply.data.api.SalaryApiClient
import com.vwatek.apply.data.api.SalaryInsightsResponse
import com.vwatek.apply.data.api.OfferEvaluationResponse
import com.vwatek.apply.data.api.NegotiationSessionResponse
import com.vwatek.apply.data.api.NegotiationMessageResponse
import com.vwatek.apply.data.api.OfferStatus
import com.vwatek.apply.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Phase 4: Salary Intelligence Use Cases
 * Handles salary insights, offer evaluation, and negotiation coaching
 */

// Get salary insights for a job
class GetSalaryInsightsUseCase(
    private val salaryApiClient: SalaryApiClient,
    private val subscriptionManager: SubscriptionManager
) {
    suspend operator fun invoke(
        jobTitle: String,
        location: String,
        yearsExperience: Int = 0,
        skills: List<String> = emptyList(),
        company: String? = null,
        industry: String? = null
    ): Result<SalaryInsightsResponse> {
        // Check if user has access to salary insights
        if (!subscriptionManager.canUseFeature(PremiumFeature.SALARY_INSIGHTS)) {
            return Result.failure(
                FeatureNotAvailableException(
                    feature = PremiumFeature.SALARY_INSIGHTS,
                    requiredTier = SubscriptionTier.PRO
                )
            )
        }
        
        // Validate input
        if (jobTitle.isBlank()) {
            return Result.failure(IllegalArgumentException("Job title is required"))
        }
        if (location.isBlank()) {
            return Result.failure(IllegalArgumentException("Location is required"))
        }
        
        return salaryApiClient.getSalaryInsights(
            jobTitle = jobTitle,
            location = location,
            yearsExperience = yearsExperience,
            skills = skills,
            company = company,
            industry = industry
        )
    }
}

// Get salary search history
class GetSalaryHistoryUseCase(
    private val salaryApiClient: SalaryApiClient
) {
    suspend operator fun invoke(): Result<List<SalaryInsightsResponse>> {
        return salaryApiClient.getSalaryHistory()
    }
}

// Evaluate a job offer
class EvaluateOfferUseCase(
    private val salaryApiClient: SalaryApiClient,
    private val subscriptionManager: SubscriptionManager
) {
    suspend operator fun invoke(
        offer: JobOffer
    ): Result<OfferEvaluationResponse> {
        // Check premium feature access
        if (!subscriptionManager.canUseFeature(PremiumFeature.SALARY_INSIGHTS)) {
            return Result.failure(
                FeatureNotAvailableException(
                    feature = PremiumFeature.SALARY_INSIGHTS,
                    requiredTier = SubscriptionTier.PRO
                )
            )
        }
        
        // Validate offer
        if (offer.companyName.isBlank()) {
            return Result.failure(IllegalArgumentException("Company name is required"))
        }
        if (offer.jobTitle.isBlank()) {
            return Result.failure(IllegalArgumentException("Job title is required"))
        }
        if (offer.baseSalary <= 0) {
            return Result.failure(IllegalArgumentException("Base salary must be positive"))
        }
        
        return salaryApiClient.evaluateOffer(offer)
    }
}

// Get saved offers
class GetSavedOffersUseCase(
    private val salaryApiClient: SalaryApiClient
) {
    suspend operator fun invoke(): Result<List<JobOffer>> {
        val result = salaryApiClient.getOffers()
        return result.map { response -> response.offers }
    }
}

// Update offer status
class UpdateOfferStatusUseCase(
    private val salaryApiClient: SalaryApiClient
) {
    suspend operator fun invoke(offerId: String, status: OfferStatus): Result<String> {
        val result = salaryApiClient.updateOfferStatus(offerId, status)
        return result.map { it.message }
    }
}

/**
 * Negotiation Coach Use Cases
 */

// Start a negotiation session
class StartNegotiationSessionUseCase(
    private val salaryApiClient: SalaryApiClient,
    private val subscriptionManager: SubscriptionManager
) {
    suspend operator fun invoke(
        offerId: String
    ): Result<NegotiationSessionResponse> {
        // Check premium feature access
        if (!subscriptionManager.canUseFeature(PremiumFeature.NEGOTIATION_COACH)) {
            return Result.failure(
                FeatureNotAvailableException(
                    feature = PremiumFeature.NEGOTIATION_COACH,
                    requiredTier = SubscriptionTier.PREMIUM
                )
            )
        }
        
        return salaryApiClient.createNegotiationSession(offerId)
    }
}

// Get negotiation session
class GetNegotiationSessionUseCase(
    private val salaryApiClient: SalaryApiClient
) {
    suspend operator fun invoke(sessionId: String): Result<NegotiationSessionResponse> {
        return salaryApiClient.getNegotiationSession(sessionId)
    }
}

// Send message to negotiation coach
class SendNegotiationMessageUseCase(
    private val salaryApiClient: SalaryApiClient,
    private val subscriptionManager: SubscriptionManager
) {
    suspend operator fun invoke(
        sessionId: String,
        message: String
    ): Result<NegotiationMessageResponse> {
        // Check premium feature access
        if (!subscriptionManager.canUseFeature(PremiumFeature.NEGOTIATION_COACH)) {
            return Result.failure(
                FeatureNotAvailableException(
                    feature = PremiumFeature.NEGOTIATION_COACH,
                    requiredTier = SubscriptionTier.PREMIUM
                )
            )
        }
        
        if (message.isBlank()) {
            return Result.failure(IllegalArgumentException("Message cannot be empty"))
        }
        
        return salaryApiClient.sendNegotiationMessage(sessionId, message)
    }
}

/**
 * Salary Intelligence Manager - provides reactive state for salary features
 */
class SalaryIntelligenceManager(
    private val salaryApiClient: SalaryApiClient,
    private val subscriptionManager: SubscriptionManager
) {
    private val _insightsState = MutableStateFlow<SalaryInsightsState>(SalaryInsightsState.Idle)
    val insightsState: StateFlow<SalaryInsightsState> = _insightsState
    
    private val _offersState = MutableStateFlow<OffersState>(OffersState.Loading)
    val offersState: StateFlow<OffersState> = _offersState
    
    private val _negotiationState = MutableStateFlow<NegotiationState>(NegotiationState.Idle)
    val negotiationState: StateFlow<NegotiationState> = _negotiationState
    
    suspend fun searchSalary(
        jobTitle: String,
        location: String,
        yearsExperience: Int = 0,
        skills: List<String> = emptyList()
    ) {
        _insightsState.value = SalaryInsightsState.Loading
        
        salaryApiClient.getSalaryInsights(
            jobTitle = jobTitle,
            location = location,
            yearsExperience = yearsExperience,
            skills = skills
        ).onSuccess { response ->
            _insightsState.value = SalaryInsightsState.Success(response)
        }.onFailure { error ->
            _insightsState.value = SalaryInsightsState.Error(error.message ?: "Failed to get salary insights")
        }
    }
    
    suspend fun loadOffers() {
        _offersState.value = OffersState.Loading
        
        salaryApiClient.getOffers()
            .onSuccess { response ->
                _offersState.value = OffersState.Success(response.offers)
            }
            .onFailure { error ->
                _offersState.value = OffersState.Error(error.message ?: "Failed to load offers")
            }
    }
    
    suspend fun evaluateOffer(offer: JobOffer) {
        _insightsState.value = SalaryInsightsState.Loading
        
        salaryApiClient.evaluateOffer(offer)
            .onSuccess { response ->
                _insightsState.value = SalaryInsightsState.OfferEvaluated(response)
            }
            .onFailure { error ->
                _insightsState.value = SalaryInsightsState.Error(error.message ?: "Failed to evaluate offer")
            }
    }
    
    suspend fun startNegotiation(offerId: String) {
        _negotiationState.value = NegotiationState.Loading
        
        salaryApiClient.createNegotiationSession(offerId)
            .onSuccess { session ->
                _negotiationState.value = NegotiationState.Active(session)
            }
            .onFailure { error ->
                _negotiationState.value = NegotiationState.Error(error.message ?: "Failed to start negotiation")
            }
    }
    
    suspend fun sendMessage(sessionId: String, message: String) {
        val currentState = _negotiationState.value
        if (currentState !is NegotiationState.Active) return
        
        salaryApiClient.sendNegotiationMessage(sessionId, message)
            .onSuccess { response ->
                // Refresh session to get updated messages
                salaryApiClient.getNegotiationSession(sessionId)
                    .onSuccess { session ->
                        _negotiationState.value = NegotiationState.Active(session)
                    }
            }
            .onFailure { error ->
                _negotiationState.value = NegotiationState.Error(error.message ?: "Failed to send message")
            }
    }
    
    fun clearInsights() {
        _insightsState.value = SalaryInsightsState.Idle
    }
    
    fun endNegotiation() {
        _negotiationState.value = NegotiationState.Idle
    }
}

sealed class SalaryInsightsState {
    data object Idle : SalaryInsightsState()
    data object Loading : SalaryInsightsState()
    data class Success(val insights: SalaryInsightsResponse) : SalaryInsightsState()
    data class OfferEvaluated(val evaluation: OfferEvaluationResponse) : SalaryInsightsState()
    data class Error(val message: String) : SalaryInsightsState()
}

sealed class OffersState {
    data object Loading : OffersState()
    data class Success(val offers: List<JobOffer>) : OffersState()
    data class Error(val message: String) : OffersState()
}

sealed class NegotiationState {
    data object Idle : NegotiationState()
    data object Loading : NegotiationState()
    data class Active(val session: NegotiationSessionResponse) : NegotiationState()
    data class Error(val message: String) : NegotiationState()
}

/**
 * Exception for premium feature access denial
 */
class FeatureNotAvailableException(
    val feature: PremiumFeature,
    val requiredTier: SubscriptionTier
) : Exception("Feature ${feature.name} requires ${requiredTier.name} tier")
