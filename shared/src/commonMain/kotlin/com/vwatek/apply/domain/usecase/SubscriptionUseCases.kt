package com.vwatek.apply.domain.usecase

import com.vwatek.apply.data.api.SubscriptionApiClient
import com.vwatek.apply.data.api.SubscriptionResponse
import com.vwatek.apply.data.api.PricingResponse
import com.vwatek.apply.data.api.UsageLimitsResponse
import com.vwatek.apply.data.api.FeatureAccessResponse
import com.vwatek.apply.data.api.CheckoutSessionResponse
import com.vwatek.apply.data.api.PortalSessionResponse
import com.vwatek.apply.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Phase 4: Subscription Use Cases
 * Handles subscription management, pricing, and feature access
 */

// Get current subscription
class GetSubscriptionUseCase(
    private val subscriptionApiClient: SubscriptionApiClient
) {
    suspend operator fun invoke(): Result<SubscriptionResponse> {
        return subscriptionApiClient.getSubscription()
    }
}

// Get pricing tiers
class GetPricingUseCase(
    private val subscriptionApiClient: SubscriptionApiClient
) {
    suspend operator fun invoke(): Result<PricingResponse> {
        return subscriptionApiClient.getPricing()
    }
}

// Create checkout session for upgrade
class CreateCheckoutSessionUseCase(
    private val subscriptionApiClient: SubscriptionApiClient
) {
    suspend operator fun invoke(
        tier: SubscriptionTier,
        billingPeriod: BillingPeriod,
        successUrl: String,
        cancelUrl: String
    ): Result<CheckoutSessionResponse> {
        if (tier == SubscriptionTier.FREE) {
            return Result.failure(IllegalArgumentException("Cannot checkout for free tier"))
        }
        
        return subscriptionApiClient.createCheckoutSession(
            tier = tier,
            billingPeriod = billingPeriod,
            successUrl = successUrl,
            cancelUrl = cancelUrl
        )
    }
}

// Create portal session for managing billing
class CreatePortalSessionUseCase(
    private val subscriptionApiClient: SubscriptionApiClient
) {
    suspend operator fun invoke(returnUrl: String): Result<PortalSessionResponse> {
        return subscriptionApiClient.createPortalSession(returnUrl)
    }
}

// Cancel subscription
class CancelSubscriptionUseCase(
    private val subscriptionApiClient: SubscriptionApiClient
) {
    suspend operator fun invoke(): Result<String> {
        val result = subscriptionApiClient.cancelSubscription()
        return result.map { it.message }
    }
}

// Reactivate subscription
class ReactivateSubscriptionUseCase(
    private val subscriptionApiClient: SubscriptionApiClient
) {
    suspend operator fun invoke(): Result<String> {
        val result = subscriptionApiClient.reactivateSubscription()
        return result.map { it.message }
    }
}

// Get usage and limits
class GetUsageLimitsUseCase(
    private val subscriptionApiClient: SubscriptionApiClient
) {
    suspend operator fun invoke(): Result<UsageLimitsResponse> {
        return subscriptionApiClient.getUsageLimits()
    }
}

// Check feature access
class CheckFeatureAccessUseCase(
    private val subscriptionApiClient: SubscriptionApiClient
) {
    suspend operator fun invoke(feature: PremiumFeature): Result<FeatureAccessResponse> {
        return subscriptionApiClient.checkFeatureAccess(feature)
    }
}

/**
 * Subscription Manager - provides reactive state for subscription status
 * 
 * Supports demo mode for testing and early access without real payments.
 * When demo mode is enabled, all users get PREMIUM features.
 */
class SubscriptionManager(
    private val subscriptionApiClient: SubscriptionApiClient
) {
    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Loading)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState
    
    private val _usageState = MutableStateFlow<UsageLimitsResponse?>(null)
    val usageState: StateFlow<UsageLimitsResponse?> = _usageState
    
    /**
     * Demo Mode Configuration
     * When enabled, all users receive PREMIUM tier features without payment.
     * Set to false in production with real Stripe integration.
     */
    companion object {
        /**
         * Enable demo mode for testing/early access.
         * Set to false when Stripe integration is fully configured.
         */
        var isDemoMode: Boolean = true
        
        /**
         * Demo tier to grant users (PREMIUM by default)
         */
        var demoTier: SubscriptionTier = SubscriptionTier.PREMIUM
        
        /**
         * Demo mode expiry message (shown in UI)
         */
        const val DEMO_MODE_MESSAGE = "🎉 Demo Mode: Enjoy all Premium features free during our beta!"
        
        /**
         * Configure demo mode
         */
        fun configureDemoMode(enabled: Boolean, tier: SubscriptionTier = SubscriptionTier.PREMIUM) {
            isDemoMode = enabled
            demoTier = tier
        }
    }
    
    /**
     * Check if currently in demo mode
     */
    val isInDemoMode: Boolean get() = isDemoMode
    
    suspend fun refreshSubscription() {
        _subscriptionState.value = SubscriptionState.Loading
        
        // If demo mode, grant demo tier immediately
        if (isDemoMode) {
            _subscriptionState.value = SubscriptionState.Success(
                subscription = null,
                tier = demoTier,
                limits = FeatureLimits.forTier(demoTier),
                pricing = SubscriptionPricing.forTier(demoTier),
                isDemoMode = true
            )
            return
        }
        
        subscriptionApiClient.getSubscription()
            .onSuccess { response ->
                val tier = SubscriptionTier.valueOf(response.subscription.tier)
                _subscriptionState.value = SubscriptionState.Success(
                    subscription = response.subscription,
                    tier = tier,
                    limits = response.limits,
                    pricing = response.pricing,
                    isDemoMode = false
                )
            }
            .onFailure { error ->
                // Default to free tier on error
                _subscriptionState.value = SubscriptionState.Success(
                    subscription = null,
                    tier = SubscriptionTier.FREE,
                    limits = FeatureLimits.forTier(SubscriptionTier.FREE),
                    pricing = null,
                    isDemoMode = false
                )
            }
    }
    
    suspend fun refreshUsage() {
        // In demo mode, usage is unlimited
        if (isDemoMode) return
        
        subscriptionApiClient.getUsageLimits()
            .onSuccess { _usageState.value = it }
    }
    
    fun canUseFeature(feature: PremiumFeature): Boolean {
        // Demo mode grants all features
        if (isDemoMode) return true
        
        val state = _subscriptionState.value
        if (state !is SubscriptionState.Success) return false
        
        return when (feature) {
            PremiumFeature.SALARY_INSIGHTS -> state.limits.salaryInsightsAccess
            PremiumFeature.NEGOTIATION_COACH -> state.limits.negotiationCoachAccess
            PremiumFeature.LINKEDIN_OPTIMIZER -> state.limits.linkedInOptimizerAccess
            PremiumFeature.UNLIMITED_APPLICATIONS -> state.limits.unlimitedApplicationTracking
            PremiumFeature.UNLIMITED_AI_ENHANCEMENTS -> state.limits.aiEnhancementsPerDay == Int.MAX_VALUE
            PremiumFeature.UNLIMITED_RESUMES -> state.limits.resumeVersionsPerMonth == Int.MAX_VALUE
            PremiumFeature.UNLIMITED_COVER_LETTERS -> state.limits.coverLettersPerMonth == Int.MAX_VALUE
            PremiumFeature.UNLIMITED_INTERVIEWS -> state.limits.interviewSessionsPerMonth == Int.MAX_VALUE
        }
    }
    
    fun getCurrentTier(): SubscriptionTier {
        if (isDemoMode) return demoTier
        
        return when (val state = _subscriptionState.value) {
            is SubscriptionState.Success -> state.tier
            else -> SubscriptionTier.FREE
        }
    }
    
    fun getRequiredTierForFeature(feature: PremiumFeature): SubscriptionTier {
        return when (feature) {
            PremiumFeature.SALARY_INSIGHTS -> SubscriptionTier.PRO
            PremiumFeature.NEGOTIATION_COACH -> SubscriptionTier.PREMIUM
            PremiumFeature.LINKEDIN_OPTIMIZER -> SubscriptionTier.PREMIUM
            PremiumFeature.UNLIMITED_APPLICATIONS -> SubscriptionTier.PRO
            PremiumFeature.UNLIMITED_AI_ENHANCEMENTS -> SubscriptionTier.PREMIUM
            PremiumFeature.UNLIMITED_RESUMES -> SubscriptionTier.PREMIUM
            PremiumFeature.UNLIMITED_COVER_LETTERS -> SubscriptionTier.PREMIUM
            PremiumFeature.UNLIMITED_INTERVIEWS -> SubscriptionTier.PREMIUM
        }
    }
}

sealed class SubscriptionState {
    data object Loading : SubscriptionState()
    data class Success(
        val subscription: com.vwatek.apply.data.api.SubscriptionDto?,
        val tier: SubscriptionTier,
        val limits: FeatureLimits,
        val pricing: SubscriptionPricing?,
        val isDemoMode: Boolean = false
    ) : SubscriptionState()
    data class Error(val message: String) : SubscriptionState()
}
