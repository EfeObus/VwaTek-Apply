package com.vwatek.apply.domain.model

import kotlinx.serialization.Serializable

/**
 * Subscription Tiers for VwaTek Apply
 * - FREE: Limited features
 * - PRO: Professional features ($14.99/mo or $149.99/yr)
 * - PREMIUM: All features + AI coaching ($29.99/mo or $299.99/yr)
 */
@Serializable
enum class SubscriptionTier {
    FREE,
    PRO,
    PREMIUM
}

/**
 * Feature limits per subscription tier
 */
@Serializable
data class FeatureLimits(
    val resumeVersionsPerMonth: Int,
    val aiEnhancementsPerDay: Int,
    val coverLettersPerMonth: Int,
    val interviewSessionsPerMonth: Int,
    val salaryInsightsAccess: Boolean,
    val negotiationCoachAccess: Boolean,
    val linkedInOptimizerAccess: Boolean,
    val jobBankIntegration: Boolean,
    val nocCodeLookup: Boolean,
    val unlimitedApplicationTracking: Boolean
) {
    companion object {
        fun forTier(tier: SubscriptionTier): FeatureLimits = when (tier) {
            SubscriptionTier.FREE -> FeatureLimits(
                resumeVersionsPerMonth = 3,
                aiEnhancementsPerDay = 2,
                coverLettersPerMonth = 5,
                interviewSessionsPerMonth = 3,
                salaryInsightsAccess = false,
                negotiationCoachAccess = false,
                linkedInOptimizerAccess = false,
                jobBankIntegration = true,
                nocCodeLookup = true,
                unlimitedApplicationTracking = false
            )
            SubscriptionTier.PRO -> FeatureLimits(
                resumeVersionsPerMonth = 15,
                aiEnhancementsPerDay = 10,
                coverLettersPerMonth = 20,
                interviewSessionsPerMonth = 15,
                salaryInsightsAccess = true,
                negotiationCoachAccess = false,
                linkedInOptimizerAccess = false,
                jobBankIntegration = true,
                nocCodeLookup = true,
                unlimitedApplicationTracking = true
            )
            SubscriptionTier.PREMIUM -> FeatureLimits(
                resumeVersionsPerMonth = Int.MAX_VALUE,
                aiEnhancementsPerDay = Int.MAX_VALUE,
                coverLettersPerMonth = Int.MAX_VALUE,
                interviewSessionsPerMonth = Int.MAX_VALUE,
                salaryInsightsAccess = true,
                negotiationCoachAccess = true,
                linkedInOptimizerAccess = true,
                jobBankIntegration = true,
                nocCodeLookup = true,
                unlimitedApplicationTracking = true
            )
        }
    }
}

/**
 * Billing period options
 */
@Serializable
enum class BillingPeriod {
    MONTHLY,
    YEARLY
}

/**
 * Subscription status
 */
@Serializable
enum class SubscriptionStatus {
    ACTIVE,
    TRIALING,
    PAST_DUE,
    CANCELED,
    INCOMPLETE,
    INCOMPLETE_EXPIRED,
    UNPAID,
    PAUSED
}

/**
 * Payment provider type
 */
@Serializable
enum class PaymentProvider {
    STRIPE,
    APPLE_IAP,
    GOOGLE_PLAY
}

/**
 * Pricing information for display
 */
@Serializable
data class SubscriptionPricing(
    val tier: SubscriptionTier,
    val monthlyPriceCad: Double,
    val yearlyPriceCad: Double,
    val monthlyPriceUsd: Double,
    val yearlyPriceUsd: Double,
    val stripePriceIdMonthly: String,
    val stripePriceIdYearly: String,
    val appleProductIdMonthly: String,
    val appleProductIdYearly: String,
    val googleProductIdMonthly: String,
    val googleProductIdYearly: String
) {
    companion object {
        val PRO = SubscriptionPricing(
            tier = SubscriptionTier.PRO,
            monthlyPriceCad = 14.99,
            yearlyPriceCad = 149.99,
            monthlyPriceUsd = 11.99,
            yearlyPriceUsd = 119.99,
            stripePriceIdMonthly = "price_1T08Y3KEHccseOSEkaoLPcrx",
            stripePriceIdYearly = "price_1T08buKEHccseOSErXHStkJX",
            appleProductIdMonthly = "com.vwatek.apply.pro.monthly",
            appleProductIdYearly = "com.vwatek.apply.pro.yearly",
            googleProductIdMonthly = "pro_monthly",
            googleProductIdYearly = "pro_yearly"
        )
        
        val PREMIUM = SubscriptionPricing(
            tier = SubscriptionTier.PREMIUM,
            monthlyPriceCad = 29.99,
            yearlyPriceCad = 299.99,
            monthlyPriceUsd = 24.99,
            yearlyPriceUsd = 249.99,
            stripePriceIdMonthly = "price_1T08ckKEHccseOSEPYmAARuB",
            stripePriceIdYearly = "price_1T08dRKEHccseOSEEU5D0jQl",
            appleProductIdMonthly = "com.vwatek.apply.premium.monthly",
            appleProductIdYearly = "com.vwatek.apply.premium.yearly",
            googleProductIdMonthly = "premium_monthly",
            googleProductIdYearly = "premium_yearly"
        )
        
        fun forTier(tier: SubscriptionTier): SubscriptionPricing? = when (tier) {
            SubscriptionTier.FREE -> null
            SubscriptionTier.PRO -> PRO
            SubscriptionTier.PREMIUM -> PREMIUM
        }
        
        val allPricing = listOf(PRO, PREMIUM)
    }
}

/**
 * Premium features that require subscription
 */
@Serializable
enum class PremiumFeature {
    SALARY_INSIGHTS,
    NEGOTIATION_COACH,
    LINKEDIN_OPTIMIZER,
    UNLIMITED_APPLICATIONS,
    UNLIMITED_AI_ENHANCEMENTS,
    UNLIMITED_RESUMES,
    UNLIMITED_COVER_LETTERS,
    UNLIMITED_INTERVIEWS
}

/**
 * Check if feature is available for a subscription tier
 */
fun isFeatureAvailable(tier: SubscriptionTier, feature: PremiumFeature): Boolean {
    val limits = FeatureLimits.forTier(tier)
    return when (feature) {
        PremiumFeature.SALARY_INSIGHTS -> limits.salaryInsightsAccess
        PremiumFeature.NEGOTIATION_COACH -> limits.negotiationCoachAccess
        PremiumFeature.LINKEDIN_OPTIMIZER -> limits.linkedInOptimizerAccess
        PremiumFeature.UNLIMITED_APPLICATIONS -> limits.unlimitedApplicationTracking
        PremiumFeature.UNLIMITED_AI_ENHANCEMENTS -> limits.aiEnhancementsPerDay == Int.MAX_VALUE
        PremiumFeature.UNLIMITED_RESUMES -> limits.resumeVersionsPerMonth == Int.MAX_VALUE
        PremiumFeature.UNLIMITED_COVER_LETTERS -> limits.coverLettersPerMonth == Int.MAX_VALUE
        PremiumFeature.UNLIMITED_INTERVIEWS -> limits.interviewSessionsPerMonth == Int.MAX_VALUE
    }
}
