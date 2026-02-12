package com.vwatek.apply.analytics

import android.util.Log

/**
 * Android implementation of Analytics
 * 
 * This is a logging-based implementation for the shared module.
 * Firebase Analytics should be configured in the androidApp module
 * and can call through to this or implement its own tracking.
 */
actual object Analytics {
    private const val TAG = "VwaTekAnalytics"
    private var isEnabled = true
    private var currentUserId: String? = null
    private val userProperties = mutableMapOf<String, String?>()
    
    /**
     * Initialize Analytics
     */
    actual fun initialize(userId: String?, enabled: Boolean) {
        isEnabled = enabled
        userId?.let { setUserId(it) }
        Log.d(TAG, "Analytics initialized. Collection enabled: $enabled")
    }
    
    /**
     * Track an event with optional parameters
     */
    actual fun trackEvent(eventName: String, params: Map<String, Any>?) {
        if (!isEnabled) return
        Log.d(TAG, "Event tracked: $eventName with params: $params")
    }
    
    /**
     * Track a screen view
     */
    actual fun trackScreenView(screenName: String, screenClass: String?) {
        if (!isEnabled) return
        Log.d(TAG, "Screen view tracked: $screenName (class: $screenClass)")
    }
    
    /**
     * Set user ID for analytics association
     */
    actual fun setUserId(userId: String?) {
        currentUserId = userId
        Log.d(TAG, "User ID set: ${userId?.take(8)}...")
    }
    
    /**
     * Set a user property for segmentation
     */
    actual fun setUserProperty(name: String, value: String?) {
        if (!isEnabled) return
        userProperties[name] = value
        Log.d(TAG, "User property set: $name = $value")
    }
    
    /**
     * Enable or disable analytics collection
     */
    actual fun setAnalyticsEnabled(enabled: Boolean) {
        isEnabled = enabled
        Log.d(TAG, "Analytics collection ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Reset analytics data (useful for logout)
     */
    actual fun resetAnalyticsData() {
        currentUserId = null
        userProperties.clear()
        Log.d(TAG, "Analytics data reset")
    }
}
