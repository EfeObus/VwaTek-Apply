package com.vwatek.apply.analytics

/**
 * iOS implementation of Analytics
 * 
 * This is a logging-based implementation for the shared module.
 * Firebase Analytics should be configured in the iOS app via Swift
 * and can call through to this or implement its own tracking.
 */
actual object Analytics {
    private var isEnabled = true
    private var currentUserId: String? = null
    private val userProperties = mutableMapOf<String, String?>()
    
    /**
     * Initialize Analytics
     */
    actual fun initialize(userId: String?, enabled: Boolean) {
        isEnabled = enabled
        userId?.let { setUserId(it) }
        println("VwaTekAnalytics: Initialized. Collection enabled: $enabled")
    }
    
    /**
     * Track an event with optional parameters
     */
    actual fun trackEvent(eventName: String, params: Map<String, Any>?) {
        if (!isEnabled) return
        println("VwaTekAnalytics: Event tracked: $eventName with params: $params")
    }
    
    /**
     * Track a screen view
     */
    actual fun trackScreenView(screenName: String, screenClass: String?) {
        if (!isEnabled) return
        println("VwaTekAnalytics: Screen view tracked: $screenName (class: $screenClass)")
    }
    
    /**
     * Set user ID for analytics association
     */
    actual fun setUserId(userId: String?) {
        currentUserId = userId
        println("VwaTekAnalytics: User ID set: ${userId?.take(8)}...")
    }
    
    /**
     * Set a user property for segmentation
     */
    actual fun setUserProperty(name: String, value: String?) {
        if (!isEnabled) return
        userProperties[name] = value
        println("VwaTekAnalytics: User property set: $name = $value")
    }
    
    /**
     * Enable or disable analytics collection
     */
    actual fun setAnalyticsEnabled(enabled: Boolean) {
        isEnabled = enabled
        println("VwaTekAnalytics: Collection ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Reset analytics data (useful for logout)
     */
    actual fun resetAnalyticsData() {
        currentUserId = null
        userProperties.clear()
        println("VwaTekAnalytics: Data reset")
    }
}
