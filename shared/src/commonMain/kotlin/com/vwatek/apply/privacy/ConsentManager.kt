package com.vwatek.apply.privacy

import kotlinx.serialization.Serializable

/**
 * PIPEDA (Personal Information Protection and Electronic Documents Act) Consent System
 * 
 * Implements Canadian privacy law requirements:
 * - Meaningful consent for data collection
 * - Purpose limitation
 * - Right to access personal information
 * - Right to withdraw consent
 * - Right to data deletion
 * 
 * Reference: https://www.priv.gc.ca/en/privacy-topics/privacy-laws-in-canada/the-personal-information-protection-and-electronic-documents-act-pipeda/
 */

/**
 * Types of data collection purposes
 */
enum class ConsentPurpose {
    ESSENTIAL,           // Required for app functionality
    ANALYTICS,           // Usage analytics and crash reporting
    PERSONALIZATION,     // AI-powered personalization
    MARKETING,           // Marketing communications
    THIRD_PARTY_SHARING  // Sharing with third parties (job boards, etc.)
}

/**
 * Consent status for a purpose
 */
enum class ConsentStatus {
    GRANTED,
    DENIED,
    NOT_SET,
    WITHDRAWN
}

/**
 * Detailed consent record for audit trail
 */
@Serializable
data class ConsentRecord(
    val purpose: String,
    val status: String,
    val grantedAt: Long? = null,
    val withdrawnAt: Long? = null,
    val version: String,    // Consent policy version
    val ipAddress: String?, // For audit purposes
    val userAgent: String?
)

/**
 * User consent preferences
 */
@Serializable
data class ConsentPreferences(
    val userId: String,
    val consents: Map<String, ConsentRecord>,
    val lastUpdated: Long,
    val policyVersion: String
)

/**
 * Consent request shown to user
 */
@Serializable
data class ConsentRequest(
    val purpose: ConsentPurpose,
    val title: String,
    val description: String,
    val isRequired: Boolean,
    val dataCollected: List<String>,
    val retentionPeriod: String,
    val thirdParties: List<String>?
)

/**
 * PIPEDA-compliant consent configuration
 */
object PIPEDAConsent {
    const val POLICY_VERSION = "1.0.0"
    
    /**
     * Get consent requests for all purposes
     */
    fun getConsentRequests(): List<ConsentRequest> = listOf(
        ConsentRequest(
            purpose = ConsentPurpose.ESSENTIAL,
            title = "Essential Services",
            description = "Required to provide core app functionality including authentication, data storage, and resume management.",
            isRequired = true,
            dataCollected = listOf(
                "Email address",
                "Name",
                "Resume content",
                "Cover letters",
                "Interview responses"
            ),
            retentionPeriod = "Until account deletion",
            thirdParties = null
        ),
        ConsentRequest(
            purpose = ConsentPurpose.ANALYTICS,
            title = "Analytics & Improvement",
            description = "Helps us understand how you use the app to improve features and fix issues. No personal data is shared with third parties.",
            isRequired = false,
            dataCollected = listOf(
                "App usage patterns",
                "Feature usage statistics",
                "Crash reports (anonymized)",
                "Device information"
            ),
            retentionPeriod = "2 years",
            thirdParties = listOf(
                "Firebase Analytics (Google)",
                "Sentry (error tracking)"
            )
        ),
        ConsentRequest(
            purpose = ConsentPurpose.PERSONALIZATION,
            title = "AI Personalization",
            description = "Enables AI-powered features like resume optimization, cover letter generation, and interview coaching tailored to your profile.",
            isRequired = false,
            dataCollected = listOf(
                "Resume content analysis",
                "Job preferences",
                "Industry information",
                "Writing style patterns"
            ),
            retentionPeriod = "Until consent withdrawn",
            thirdParties = listOf(
                "Google AI (Gemini API)"
            )
        ),
        ConsentRequest(
            purpose = ConsentPurpose.MARKETING,
            title = "Marketing Communications",
            description = "Receive tips, job market insights, and promotional offers via email.",
            isRequired = false,
            dataCollected = listOf(
                "Email address",
                "Communication preferences"
            ),
            retentionPeriod = "Until unsubscribed",
            thirdParties = null
        ),
        ConsentRequest(
            purpose = ConsentPurpose.THIRD_PARTY_SHARING,
            title = "Job Board Integration",
            description = "Share your profile with job boards and recruiters to help you find opportunities.",
            isRequired = false,
            dataCollected = listOf(
                "Resume content",
                "Contact information",
                "Job preferences"
            ),
            retentionPeriod = "Until consent withdrawn",
            thirdParties = listOf(
                "Job Bank Canada",
                "LinkedIn (optional)",
                "Partner job boards"
            )
        )
    )
    
    /**
     * Validate if required consents are granted
     */
    fun areRequiredConsentsGranted(preferences: ConsentPreferences): Boolean {
        val requiredPurposes = getConsentRequests()
            .filter { it.isRequired }
            .map { it.purpose.name }
        
        return requiredPurposes.all { purpose ->
            preferences.consents[purpose]?.status == ConsentStatus.GRANTED.name
        }
    }
    
    /**
     * Check if a specific consent is granted
     */
    fun isConsentGranted(preferences: ConsentPreferences, purpose: ConsentPurpose): Boolean {
        return preferences.consents[purpose.name]?.status == ConsentStatus.GRANTED.name
    }
}

/**
 * Platform-independent consent manager interface
 */
interface ConsentManager {
    /**
     * Get current consent preferences
     */
    suspend fun getPreferences(): ConsentPreferences?
    
    /**
     * Save consent preferences
     */
    suspend fun savePreferences(preferences: ConsentPreferences)
    
    /**
     * Grant consent for a purpose
     */
    suspend fun grantConsent(
        userId: String,
        purpose: ConsentPurpose,
        ipAddress: String? = null,
        userAgent: String? = null
    )
    
    /**
     * Withdraw consent for a purpose
     */
    suspend fun withdrawConsent(userId: String, purpose: ConsentPurpose)
    
    /**
     * Check if consent is granted
     */
    suspend fun isConsentGranted(purpose: ConsentPurpose): Boolean
    
    /**
     * Check if consent flow needs to be shown
     */
    suspend fun needsConsentFlow(): Boolean
    
    /**
     * Clear all consent data (for account deletion)
     */
    suspend fun clearAllConsent()
}

/**
 * Consent manager factory
 */
expect class ConsentManagerFactory {
    fun create(): ConsentManager
}
