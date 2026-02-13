package com.vwatek.apply.data.api

import com.vwatek.apply.domain.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

/**
 * Phase 4: Subscription API Client
 * Handles subscription management, pricing, and billing operations
 */
class SubscriptionApiClient(
    private val httpClient: HttpClient
) {
    private val baseUrl = "${ApiConfig.apiV1Url}/subscriptions"
    
    // ===== Subscription Management =====
    
    /**
     * Get current user's subscription
     */
    suspend fun getSubscription(): Result<SubscriptionResponse> {
        return try {
            val response = httpClient.get(baseUrl)
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get pricing information for all tiers
     */
    suspend fun getPricing(): Result<PricingResponse> {
        return try {
            val response = httpClient.get("$baseUrl/pricing")
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a Stripe checkout session
     */
    suspend fun createCheckoutSession(
        tier: SubscriptionTier,
        billingPeriod: BillingPeriod,
        successUrl: String,
        cancelUrl: String
    ): Result<CheckoutSessionResponse> {
        return try {
            val response = httpClient.post("$baseUrl/checkout") {
                contentType(ContentType.Application.Json)
                setBody(CreateCheckoutRequest(
                    tier = tier.name,
                    billingPeriod = billingPeriod.name,
                    successUrl = successUrl,
                    cancelUrl = cancelUrl
                ))
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a customer portal session for managing subscription
     */
    suspend fun createPortalSession(returnUrl: String): Result<PortalSessionResponse> {
        return try {
            val response = httpClient.post("$baseUrl/portal") {
                contentType(ContentType.Application.Json)
                setBody(CreatePortalRequest(returnUrl))
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Cancel subscription at period end
     */
    suspend fun cancelSubscription(): Result<MessageResponse> {
        return try {
            val response = httpClient.post("$baseUrl/cancel")
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Reactivate a canceled subscription
     */
    suspend fun reactivateSubscription(): Result<MessageResponse> {
        return try {
            val response = httpClient.post("$baseUrl/reactivate")
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get usage/limits for current subscription
     */
    suspend fun getUsageLimits(): Result<UsageLimitsResponse> {
        return try {
            val response = httpClient.get("$baseUrl/usage")
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if a specific premium feature is available
     */
    suspend fun checkFeatureAccess(feature: PremiumFeature): Result<FeatureAccessResponse> {
        return try {
            val response = httpClient.get("$baseUrl/feature/${feature.name}")
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ===== Request/Response DTOs =====

@Serializable
data class SubscriptionResponse(
    val subscription: SubscriptionDto,
    val limits: FeatureLimits,
    val pricing: SubscriptionPricing?
)

@Serializable
data class SubscriptionDto(
    val id: String,
    val userId: String,
    val tier: String,
    val status: String,
    val billingPeriod: String,
    val paymentProvider: String,
    val currentPeriodStart: String,
    val currentPeriodEnd: String,
    val cancelAtPeriodEnd: Boolean,
    val canceledAt: String?,
    val trialStart: String?,
    val trialEnd: String?
)

@Serializable
data class CreateCheckoutRequest(
    val tier: String,
    val billingPeriod: String,
    val successUrl: String,
    val cancelUrl: String
)

@Serializable
data class CreatePortalRequest(
    val returnUrl: String
)

@Serializable
data class PricingResponse(
    val pricing: List<TierPricingDto>
)

@Serializable
data class TierPricingDto(
    val tier: String,
    val name: String,
    val description: String,
    val monthlyPriceCad: Double,
    val yearlyPriceCad: Double,
    val monthlyPriceUsd: Double,
    val yearlyPriceUsd: Double,
    val features: List<String>,
    val limits: FeatureLimits
)

@Serializable
data class UsageLimitsResponse(
    val tier: String,
    val limits: FeatureLimits,
    val usage: UsageStatsDto
)

@Serializable
data class UsageStatsDto(
    val periodStart: String,
    val periodEnd: String,
    val resumeVersionsUsed: Int,
    val resumeVersionsRemaining: Int,
    val aiEnhancementsUsedToday: Int,
    val aiEnhancementsRemaining: Int,
    val coverLettersUsed: Int,
    val coverLettersRemaining: Int,
    val interviewSessionsUsed: Int,
    val interviewSessionsRemaining: Int
)

@Serializable
data class FeatureAccessResponse(
    val feature: String,
    val available: Boolean,
    val requiredTier: String
)

@Serializable
data class MessageResponse(
    val message: String
)

@Serializable
data class CheckoutSessionResponse(
    val sessionId: String,
    val checkoutUrl: String
)

@Serializable
data class PortalSessionResponse(
    val sessionId: String,
    val portalUrl: String
)
