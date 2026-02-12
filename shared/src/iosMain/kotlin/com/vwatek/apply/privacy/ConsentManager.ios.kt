package com.vwatek.apply.privacy

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.*

/**
 * iOS factory for ConsentManager
 */
actual class ConsentManagerFactory {
    actual fun create(): ConsentManager {
        return IosConsentManager()
    }
}

/**
 * iOS implementation of ConsentManager using NSUserDefaults
 */
internal class IosConsentManager : ConsentManager {
    
    private val defaults = NSUserDefaults.standardUserDefaults
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    override suspend fun getPreferences(): ConsentPreferences? {
        val jsonString = defaults.stringForKey(PREFS_KEY_CONSENT) ?: return null
        return try {
            json.decodeFromString<ConsentPreferences>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun savePreferences(preferences: ConsentPreferences) {
        val jsonString = json.encodeToString(preferences)
        defaults.setObject(jsonString, PREFS_KEY_CONSENT)
    }
    
    override suspend fun grantConsent(
        userId: String,
        purpose: ConsentPurpose,
        ipAddress: String?,
        userAgent: String?
    ) {
        val currentTime = (NSDate().timeIntervalSince1970 * 1000).toLong()
        
        val currentPrefs = getPreferences() ?: ConsentPreferences(
            userId = userId,
            consents = emptyMap(),
            lastUpdated = currentTime,
            policyVersion = PIPEDAConsent.POLICY_VERSION
        )
        
        val newConsent = ConsentRecord(
            purpose = purpose.name,
            status = ConsentStatus.GRANTED.name,
            grantedAt = currentTime,
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
                lastUpdated = currentTime
            )
        )
    }
    
    override suspend fun withdrawConsent(userId: String, purpose: ConsentPurpose) {
        val currentPrefs = getPreferences() ?: return
        val currentTime = (NSDate().timeIntervalSince1970 * 1000).toLong()
        
        val existingConsent = currentPrefs.consents[purpose.name] ?: return
        
        val withdrawnConsent = existingConsent.copy(
            status = ConsentStatus.WITHDRAWN.name,
            withdrawnAt = currentTime
        )
        
        val updatedConsents = currentPrefs.consents.toMutableMap()
        updatedConsents[purpose.name] = withdrawnConsent
        
        savePreferences(
            currentPrefs.copy(
                consents = updatedConsents,
                lastUpdated = currentTime
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
        defaults.removeObjectForKey(PREFS_KEY_CONSENT)
    }
    
    companion object {
        private const val PREFS_KEY_CONSENT = "vwatek_user_consents"
    }
}
