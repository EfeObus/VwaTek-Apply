package com.vwatek.apply.privacy

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Android factory for ConsentManager
 */
actual class ConsentManagerFactory {
    private var context: Context? = null
    
    fun initialize(context: Context) {
        this.context = context.applicationContext
    }
    
    actual fun create(): ConsentManager {
        val ctx = context ?: throw IllegalStateException(
            "ConsentManagerFactory must be initialized with context first. " +
            "Call ConsentManagerFactory().initialize(context) in your Application class."
        )
        return AndroidConsentManager(ctx)
    }
}

/**
 * Android implementation of ConsentManager using SharedPreferences
 */
internal class AndroidConsentManager(
    private val context: Context
) : ConsentManager {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "vwatek_consent_prefs",
        Context.MODE_PRIVATE
    )
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    override suspend fun getPreferences(): ConsentPreferences? {
        val jsonString = prefs.getString(PREFS_KEY_CONSENT, null) ?: return null
        return try {
            json.decodeFromString<ConsentPreferences>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun savePreferences(preferences: ConsentPreferences) {
        val jsonString = json.encodeToString(preferences)
        prefs.edit().putString(PREFS_KEY_CONSENT, jsonString).apply()
    }
    
    override suspend fun grantConsent(
        userId: String,
        purpose: ConsentPurpose,
        ipAddress: String?,
        userAgent: String?
    ) {
        val currentPrefs = getPreferences() ?: ConsentPreferences(
            userId = userId,
            consents = emptyMap(),
            lastUpdated = System.currentTimeMillis(),
            policyVersion = PIPEDAConsent.POLICY_VERSION
        )
        
        val newConsent = ConsentRecord(
            purpose = purpose.name,
            status = ConsentStatus.GRANTED.name,
            grantedAt = System.currentTimeMillis(),
            withdrawnAt = null,
            version = PIPEDAConsent.POLICY_VERSION,
            ipAddress = ipAddress,
            userAgent = userAgent
        )
        
        val updatedConsents = currentPrefs.consents.toMutableMap()
        updatedConsents[purpose.name] = newConsent
        
        savePreferences(
            currentPrefs.copy(
                consents = updatedConsents,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }
    
    override suspend fun withdrawConsent(userId: String, purpose: ConsentPurpose) {
        val currentPrefs = getPreferences() ?: return
        
        val existingConsent = currentPrefs.consents[purpose.name] ?: return
        
        val withdrawnConsent = existingConsent.copy(
            status = ConsentStatus.WITHDRAWN.name,
            withdrawnAt = System.currentTimeMillis()
        )
        
        val updatedConsents = currentPrefs.consents.toMutableMap()
        updatedConsents[purpose.name] = withdrawnConsent
        
        savePreferences(
            currentPrefs.copy(
                consents = updatedConsents,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }
    
    override suspend fun isConsentGranted(purpose: ConsentPurpose): Boolean {
        val prefs = getPreferences() ?: return false
        return prefs.consents[purpose.name]?.status == ConsentStatus.GRANTED.name
    }
    
    override suspend fun needsConsentFlow(): Boolean {
        val prefs = getPreferences() ?: return true
        
        // Check if policy version changed
        if (prefs.policyVersion != PIPEDAConsent.POLICY_VERSION) {
            return true
        }
        
        // Check if all required consents are set
        return !PIPEDAConsent.areRequiredConsentsGranted(prefs)
    }
    
    override suspend fun clearAllConsent() {
        prefs.edit().remove(PREFS_KEY_CONSENT).apply()
    }
    
    companion object {
        private const val PREFS_KEY_CONSENT = "user_consents"
    }
}
