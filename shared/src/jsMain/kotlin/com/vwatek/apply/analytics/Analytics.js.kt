package com.vwatek.apply.analytics

import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlin.js.json

/**
 * JavaScript/Web implementation of Analytics
 * 
 * Uses a combination of:
 * - Custom event tracking stored locally
 * - Integration with Sentry for error correlation
 * - Console logging for development
 * 
 * In production, this can be extended to send events to a backend analytics service
 * or integrate with services like Google Analytics 4, Mixpanel, or Amplitude.
 */
actual object Analytics {
    private const val ANALYTICS_ENABLED_KEY = "vwatek_analytics_enabled"
    private const val USER_ID_KEY = "vwatek_analytics_user_id"
    private const val EVENTS_QUEUE_KEY = "vwatek_analytics_events_queue"
    
    private var isEnabled = true
    private var currentUserId: String? = null
    private val userProperties = mutableMapOf<String, String?>()
    
    /**
     * Initialize web analytics
     */
    actual fun initialize(userId: String?, enabled: Boolean) {
        isEnabled = enabled
        localStorage.getItem(ANALYTICS_ENABLED_KEY)?.let {
            isEnabled = it == "true"
        }
        
        userId?.let { setUserId(it) }
        
        // Load stored user ID
        localStorage.getItem(USER_ID_KEY)?.let {
            currentUserId = it
        }
        
        console.log("[VwaTekAnalytics] Initialized. Collection enabled: $isEnabled")
        
        // Track app initialization
        trackEvent(AnalyticsEvents.SCREEN_VIEW, mapOf(
            AnalyticsParams.SCREEN_NAME to "app_launch",
            AnalyticsParams.PLATFORM to "web"
        ))
    }
    
    /**
     * Track an event with optional parameters
     */
    actual fun trackEvent(eventName: String, params: Map<String, Any>?) {
        if (!isEnabled) return
        
        try {
            val event = createEventObject(eventName, params)
            
            // Log to console in development
            console.log("[VwaTekAnalytics] Event: $eventName", params?.toString() ?: "")
            
            // Store event for batch sending (if implementing backend analytics)
            queueEvent(event)
            
            // Integrate with Sentry breadcrumbs if available
            try {
                val sentry = js("window.Sentry")
                if (sentry != null && sentry != undefined) {
                    sentry.addBreadcrumb(json(
                        "category" to "analytics",
                        "message" to eventName,
                        "level" to "info",
                        "data" to (params ?: emptyMap<String, Any>())
                    ))
                }
            } catch (e: dynamic) {
                // Sentry not available, continue silently
            }
            
        } catch (e: Exception) {
            console.error("[VwaTekAnalytics] Failed to track event: $eventName", e.message)
        }
    }
    
    /**
     * Track a screen view
     */
    actual fun trackScreenView(screenName: String, screenClass: String?) {
        if (!isEnabled) return
        
        val params = mutableMapOf<String, Any>(
            AnalyticsParams.SCREEN_NAME to screenName
        )
        screenClass?.let { params[AnalyticsParams.SCREEN_CLASS] = it }
        
        trackEvent(AnalyticsEvents.SCREEN_VIEW, params)
        
        // Update browser document title for better tracking
        try {
            js("document.title = 'VwaTek Apply - ' + screenName")
        } catch (e: dynamic) {
            // Ignore if document is not available
        }
    }
    
    /**
     * Set user ID for analytics association
     */
    actual fun setUserId(userId: String?) {
        currentUserId = userId
        
        if (userId != null) {
            localStorage.setItem(USER_ID_KEY, userId)
        } else {
            localStorage.removeItem(USER_ID_KEY)
        }
        
        console.log("[VwaTekAnalytics] User ID set: ${userId?.take(8) ?: "null"}...")
    }
    
    /**
     * Set a user property for segmentation
     */
    actual fun setUserProperty(name: String, value: String?) {
        if (!isEnabled) return
        
        userProperties[name] = value
        console.log("[VwaTekAnalytics] User property set: $name = $value")
    }
    
    /**
     * Enable or disable analytics collection
     */
    actual fun setAnalyticsEnabled(enabled: Boolean) {
        isEnabled = enabled
        localStorage.setItem(ANALYTICS_ENABLED_KEY, enabled.toString())
        console.log("[VwaTekAnalytics] Collection ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Reset analytics data (useful for logout)
     */
    actual fun resetAnalyticsData() {
        currentUserId = null
        userProperties.clear()
        
        localStorage.removeItem(USER_ID_KEY)
        localStorage.removeItem(EVENTS_QUEUE_KEY)
        
        console.log("[VwaTekAnalytics] Data reset")
    }
    
    /**
     * Create a structured event object
     */
    private fun createEventObject(eventName: String, params: Map<String, Any>?): dynamic {
        val timestamp = js("Date.now()")
        
        return json(
            "event_name" to eventName,
            "timestamp" to timestamp,
            "user_id" to currentUserId,
            "user_properties" to userProperties.toMap(),
            "params" to (params ?: emptyMap<String, Any>()),
            "context" to json(
                "platform" to "web",
                "url" to (window.location.href),
                "user_agent" to (window.navigator.userAgent),
                "language" to (window.navigator.language),
                "screen_width" to (window.screen.width),
                "screen_height" to (window.screen.height)
            )
        )
    }
    
    /**
     * Queue event for batch sending to backend
     */
    private fun queueEvent(event: dynamic) {
        try {
            val existingEvents = localStorage.getItem(EVENTS_QUEUE_KEY)
            val events: dynamic = if (existingEvents != null) {
                JSON.parse(existingEvents)
            } else {
                js("[]")
            }
            
            events.push(event)
            
            // Keep only last 100 events to prevent storage overflow
            while (events.length > 100) {
                events.shift()
            }
            
            localStorage.setItem(EVENTS_QUEUE_KEY, JSON.stringify(events))
        } catch (e: dynamic) {
            console.warn("[VwaTekAnalytics] Failed to queue event", e)
        }
    }
    
    /**
     * Get queued events (for sending to backend)
     */
    fun getQueuedEvents(): String? {
        return localStorage.getItem(EVENTS_QUEUE_KEY)
    }
    
    /**
     * Clear queued events after successful send
     */
    fun clearQueuedEvents() {
        localStorage.removeItem(EVENTS_QUEUE_KEY)
    }
}

// External console object
external val console: Console

external interface Console {
    fun log(vararg args: Any?)
    fun error(vararg args: Any?)
    fun warn(vararg args: Any?)
}

// JSON utilities
external object JSON {
    fun parse(text: String): dynamic
    fun stringify(value: dynamic): String
}
