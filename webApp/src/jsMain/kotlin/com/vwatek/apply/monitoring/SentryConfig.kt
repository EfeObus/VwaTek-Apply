package com.vwatek.apply.monitoring

import kotlinx.browser.window

/**
 * Sentry Error Tracking configuration for Web platform.
 * 
 * To use, add the Sentry SDK to your index.html:
 * <script src="https://browser.sentry-cdn.com/7.x.x/bundle.min.js"></script>
 * 
 * Then call SentryConfig.init() in your main() function.
 */
object SentryConfig {
    private var isInitialized = false
    
    /**
     * Initialize Sentry with the provided DSN.
     * DSN should be configured via window.VWATEK_CONFIG.SENTRY_DSN
     */
    fun init() {
        if (isInitialized) return
        
        val globalConfig: dynamic = js("window.VWATEK_CONFIG")
        val sentryDsn = globalConfig?.SENTRY_DSN as? String
        
        if (sentryDsn.isNullOrBlank()) {
            console.log("Sentry DSN not configured. Error tracking disabled.")
            return
        }
        
        val environment = globalConfig?.ENVIRONMENT as? String ?: "development"
        val release = globalConfig?.VERSION as? String ?: "unknown"
        
        try {
            js("""
                if (typeof Sentry !== 'undefined') {
                    Sentry.init({
                        dsn: sentryDsn,
                        environment: environment,
                        release: 'vwatek-apply@' + release,
                        tracesSampleRate: 0.1,
                        replaysSessionSampleRate: 0.1,
                        replaysOnErrorSampleRate: 1.0,
                        integrations: [
                            Sentry.browserTracingIntegration(),
                            Sentry.replayIntegration()
                        ],
                        beforeSend: function(event) {
                            // Don't send events in development
                            if (environment === 'development') {
                                console.log('Sentry event (not sent in dev):', event);
                                return null;
                            }
                            return event;
                        }
                    });
                }
            """)
            isInitialized = true
            console.log("Sentry initialized successfully")
        } catch (e: Exception) {
            console.error("Failed to initialize Sentry: ${e.message}")
        }
    }
    
    /**
     * Capture an exception and send to Sentry
     */
    fun captureException(error: Throwable, context: Map<String, Any>? = null) {
        try {
            if (context != null) {
                val contextJson = JSON.stringify(context.toJsObject())
                js("Sentry && Sentry.setContext('custom', JSON.parse(contextJson))")
            }
            js("Sentry && Sentry.captureException(new Error(error.message || error.toString()))")
        } catch (e: Exception) {
            console.error("Failed to capture exception: ${e.message}")
        }
    }
    
    /**
     * Capture a message and send to Sentry
     */
    fun captureMessage(message: String, level: SentryLevel = SentryLevel.INFO) {
        try {
            js("Sentry && Sentry.captureMessage(message, level.name.toLowerCase())")
        } catch (e: Exception) {
            console.error("Failed to capture message: ${e.message}")
        }
    }
    
    /**
     * Set user information for crash reports
     */
    fun setUser(userId: String?, email: String? = null) {
        try {
            if (userId != null) {
                js("""
                    Sentry && Sentry.setUser({
                        id: userId,
                        email: email || undefined
                    })
                """)
            } else {
                js("Sentry && Sentry.setUser(null)")
            }
        } catch (e: Exception) {
            console.error("Failed to set user: ${e.message}")
        }
    }
    
    /**
     * Add breadcrumb for debugging crash traces
     */
    fun addBreadcrumb(message: String, category: String = "custom", level: SentryLevel = SentryLevel.INFO) {
        try {
            js("""
                Sentry && Sentry.addBreadcrumb({
                    message: message,
                    category: category,
                    level: level.name.toLowerCase()
                })
            """)
        } catch (e: Exception) {
            console.error("Failed to add breadcrumb: ${e.message}")
        }
    }
    
    /**
     * Set custom tags for filtering in Sentry dashboard
     */
    fun setTag(key: String, value: String) {
        try {
            js("Sentry && Sentry.setTag(key, value)")
        } catch (e: Exception) {
            console.error("Failed to set tag: ${e.message}")
        }
    }
    
    private fun Map<String, Any>.toJsObject(): dynamic {
        val obj = js("{}")
        forEach { (key, value) ->
            obj[key] = value
        }
        return obj
    }
}

enum class SentryLevel {
    DEBUG, INFO, WARNING, ERROR, FATAL
}

/**
 * Global error handler for uncaught exceptions
 */
fun setupGlobalErrorHandler() {
    window.onerror = { message, source, lineno, colno, error ->
        console.error("Uncaught error: $message at $source:$lineno:$colno")
        error?.let { 
            SentryConfig.captureException(
                Exception(message.toString()),
                mapOf(
                    "source" to (source ?: "unknown"),
                    "line" to (lineno ?: 0),
                    "column" to (colno ?: 0)
                )
            )
        }
        false // Let other handlers run
    }
    
    // Handle unhandled promise rejections
    js("""
        window.addEventListener('unhandledrejection', function(event) {
            console.error('Unhandled promise rejection:', event.reason);
            if (typeof Sentry !== 'undefined') {
                Sentry.captureException(event.reason);
            }
        });
    """)
}
