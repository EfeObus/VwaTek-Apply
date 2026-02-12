package com.vwatek.apply.analytics

/**
 * Cross-platform Analytics Framework for VwaTek Apply
 * 
 * Provides unified analytics tracking across Android, iOS, and Web platforms.
 * Each platform implements the actual tracking using native SDKs:
 * - Android: Firebase Analytics
 * - iOS: Firebase Analytics
 * - Web: Custom analytics with Sentry integration
 */

/**
 * Analytics event names following standard naming conventions
 */
object AnalyticsEvents {
    // Onboarding events
    const val ONBOARDING_STARTED = "onboarding_started"
    const val ONBOARDING_STEP_COMPLETED = "onboarding_step_completed"
    const val ONBOARDING_COMPLETED = "onboarding_completed"
    const val ONBOARDING_SKIPPED = "onboarding_skipped"
    
    // Authentication events
    const val SIGN_UP_STARTED = "sign_up_started"
    const val SIGN_UP_COMPLETED = "sign_up_completed"
    const val SIGN_IN_STARTED = "sign_in_started"
    const val SIGN_IN_COMPLETED = "sign_in_completed"
    const val SIGN_OUT = "sign_out"
    const val PASSWORD_RESET_REQUESTED = "password_reset_requested"
    
    // Resume events
    const val RESUME_UPLOAD_STARTED = "resume_upload_started"
    const val RESUME_UPLOAD_COMPLETED = "resume_upload_completed"
    const val RESUME_UPLOAD_FAILED = "resume_upload_failed"
    const val RESUME_PARSED = "resume_parsed"
    const val RESUME_EDITED = "resume_edited"
    const val RESUME_DOWNLOADED = "resume_downloaded"
    const val RESUME_SHARED = "resume_shared"
    
    // AI Feature events
    const val AI_RESUME_OPTIMIZATION_STARTED = "ai_resume_optimization_started"
    const val AI_RESUME_OPTIMIZATION_COMPLETED = "ai_resume_optimization_completed"
    const val AI_COVER_LETTER_STARTED = "ai_cover_letter_started"
    const val AI_COVER_LETTER_COMPLETED = "ai_cover_letter_completed"
    const val AI_INTERVIEW_PREP_STARTED = "ai_interview_prep_started"
    const val AI_INTERVIEW_PREP_COMPLETED = "ai_interview_prep_completed"
    const val AI_SUGGESTION_ACCEPTED = "ai_suggestion_accepted"
    const val AI_SUGGESTION_REJECTED = "ai_suggestion_rejected"
    
    // Job tracking events
    const val JOB_APPLICATION_CREATED = "job_application_created"
    const val JOB_APPLICATION_UPDATED = "job_application_updated"
    const val JOB_APPLICATION_STATUS_CHANGED = "job_application_status_changed"
    const val JOB_APPLICATION_DELETED = "job_application_deleted"
    const val JOB_SEARCH_PERFORMED = "job_search_performed"
    const val JOB_SAVED = "job_saved"
    const val JOB_APPLIED = "job_applied"
    
    // Canadian-specific events
    const val NOC_CODE_SEARCHED = "noc_code_searched"
    const val NOC_CODE_SELECTED = "noc_code_selected"
    const val JOB_BANK_SEARCH = "job_bank_search"
    const val JOB_BANK_JOB_VIEWED = "job_bank_job_viewed"
    const val LANGUAGE_CHANGED = "language_changed"
    
    // Screen views
    const val SCREEN_VIEW = "screen_view"
    
    // Settings events
    const val SETTINGS_CHANGED = "settings_changed"
    const val NOTIFICATIONS_ENABLED = "notifications_enabled"
    const val NOTIFICATIONS_DISABLED = "notifications_disabled"
    const val DARK_MODE_ENABLED = "dark_mode_enabled"
    const val DARK_MODE_DISABLED = "dark_mode_disabled"
    
    // Subscription events
    const val SUBSCRIPTION_PAGE_VIEWED = "subscription_page_viewed"
    const val SUBSCRIPTION_STARTED = "subscription_started"
    const val SUBSCRIPTION_COMPLETED = "subscription_completed"
    const val SUBSCRIPTION_CANCELLED = "subscription_cancelled"
    const val FREE_TRIAL_STARTED = "free_trial_started"
    
    // Sync events
    const val SYNC_STARTED = "sync_started"
    const val SYNC_COMPLETED = "sync_completed"
    const val SYNC_FAILED = "sync_failed"
    const val OFFLINE_MODE_ENTERED = "offline_mode_entered"
    const val OFFLINE_MODE_EXITED = "offline_mode_exited"
    
    // Error events
    const val ERROR_OCCURRED = "error_occurred"
    const val CRASH_DETECTED = "crash_detected"
}

/**
 * Analytics parameter names
 */
object AnalyticsParams {
    const val SCREEN_NAME = "screen_name"
    const val SCREEN_CLASS = "screen_class"
    const val STEP_NUMBER = "step_number"
    const val STEP_NAME = "step_name"
    const val METHOD = "method"
    const val SUCCESS = "success"
    const val ERROR_MESSAGE = "error_message"
    const val ERROR_CODE = "error_code"
    const val FILE_TYPE = "file_type"
    const val FILE_SIZE = "file_size"
    const val RESUME_ID = "resume_id"
    const val JOB_ID = "job_id"
    const val JOB_TITLE = "job_title"
    const val COMPANY_NAME = "company_name"
    const val STATUS = "status"
    const val PREVIOUS_STATUS = "previous_status"
    const val NEW_STATUS = "new_status"
    const val AI_MODEL = "ai_model"
    const val RESPONSE_TIME_MS = "response_time_ms"
    const val TOKEN_COUNT = "token_count"
    const val NOC_CODE = "noc_code"
    const val NOC_TITLE = "noc_title"
    const val SEARCH_QUERY = "search_query"
    const val RESULTS_COUNT = "results_count"
    const val LANGUAGE = "language"
    const val SETTING_NAME = "setting_name"
    const val SETTING_VALUE = "setting_value"
    const val SUBSCRIPTION_TIER = "subscription_tier"
    const val SUBSCRIPTION_PRICE = "subscription_price"
    const val CURRENCY = "currency"
    const val SYNC_TYPE = "sync_type"
    const val ITEMS_SYNCED = "items_synced"
    const val DURATION_MS = "duration_ms"
    const val PLATFORM = "platform"
    const val APP_VERSION = "app_version"
    const val DEVICE_TYPE = "device_type"
    const val OS_VERSION = "os_version"
}

/**
 * User properties for analytics segmentation
 */
object UserProperties {
    const val USER_ID = "user_id"
    const val ACCOUNT_TYPE = "account_type"
    const val SUBSCRIPTION_STATUS = "subscription_status"
    const val SUBSCRIPTION_TIER = "subscription_tier"
    const val SIGNUP_DATE = "signup_date"
    const val RESUMES_COUNT = "resumes_count"
    const val APPLICATIONS_COUNT = "applications_count"
    const val PREFERRED_LANGUAGE = "preferred_language"
    const val COUNTRY = "country"
    const val PROVINCE = "province"
    const val INDUSTRY = "industry"
    const val JOB_SEEKING_STATUS = "job_seeking_status"
}

/**
 * Platform-independent Analytics interface
 * Implemented via expect/actual pattern for each platform
 */
expect object Analytics {
    /**
     * Initialize analytics. Should be called once at app startup.
     * @param userId Optional user ID for logged-in users
     * @param enabled Whether analytics collection is enabled
     */
    fun initialize(userId: String? = null, enabled: Boolean = true)
    
    /**
     * Track an event with optional parameters
     * @param eventName The name of the event (use AnalyticsEvents constants)
     * @param params Optional map of parameters (use AnalyticsParams constants)
     */
    fun trackEvent(eventName: String, params: Map<String, Any>? = null)
    
    /**
     * Track a screen view
     * @param screenName The name of the screen
     * @param screenClass Optional class name of the screen
     */
    fun trackScreenView(screenName: String, screenClass: String? = null)
    
    /**
     * Set user ID for analytics association
     * @param userId The unique user identifier, or null to clear
     */
    fun setUserId(userId: String?)
    
    /**
     * Set a user property for segmentation
     * @param name The property name (use UserProperties constants)
     * @param value The property value
     */
    fun setUserProperty(name: String, value: String?)
    
    /**
     * Enable or disable analytics collection
     * @param enabled Whether to enable analytics
     */
    fun setAnalyticsEnabled(enabled: Boolean)
    
    /**
     * Reset analytics data (useful for logout)
     */
    fun resetAnalyticsData()
}

/**
 * Extension functions for common analytics operations
 */

/**
 * Track onboarding progress
 */
fun Analytics.trackOnboardingStep(stepNumber: Int, stepName: String) {
    trackEvent(
        AnalyticsEvents.ONBOARDING_STEP_COMPLETED,
        mapOf(
            AnalyticsParams.STEP_NUMBER to stepNumber,
            AnalyticsParams.STEP_NAME to stepName
        )
    )
}

/**
 * Track authentication events
 */
fun Analytics.trackSignIn(method: String, success: Boolean, errorMessage: String? = null) {
    val params = mutableMapOf<String, Any>(
        AnalyticsParams.METHOD to method,
        AnalyticsParams.SUCCESS to success
    )
    errorMessage?.let { params[AnalyticsParams.ERROR_MESSAGE] = it }
    
    trackEvent(
        if (success) AnalyticsEvents.SIGN_IN_COMPLETED else AnalyticsEvents.ERROR_OCCURRED,
        params
    )
}

/**
 * Track AI feature usage
 */
fun Analytics.trackAIFeature(
    featureType: String,
    completed: Boolean,
    responseTimeMs: Long,
    tokenCount: Int? = null
) {
    val params = mutableMapOf<String, Any>(
        AnalyticsParams.SUCCESS to completed,
        AnalyticsParams.RESPONSE_TIME_MS to responseTimeMs
    )
    tokenCount?.let { params[AnalyticsParams.TOKEN_COUNT] = it }
    
    val eventName = when {
        featureType == "resume_optimization" && completed -> AnalyticsEvents.AI_RESUME_OPTIMIZATION_COMPLETED
        featureType == "cover_letter" && completed -> AnalyticsEvents.AI_COVER_LETTER_COMPLETED
        featureType == "interview_prep" && completed -> AnalyticsEvents.AI_INTERVIEW_PREP_COMPLETED
        else -> AnalyticsEvents.ERROR_OCCURRED
    }
    
    trackEvent(eventName, params)
}

/**
 * Track job application status changes
 */
fun Analytics.trackApplicationStatusChange(
    jobId: String,
    jobTitle: String,
    previousStatus: String,
    newStatus: String
) {
    trackEvent(
        AnalyticsEvents.JOB_APPLICATION_STATUS_CHANGED,
        mapOf(
            AnalyticsParams.JOB_ID to jobId,
            AnalyticsParams.JOB_TITLE to jobTitle,
            AnalyticsParams.PREVIOUS_STATUS to previousStatus,
            AnalyticsParams.NEW_STATUS to newStatus
        )
    )
}

/**
 * Track NOC code interaction (Canadian-specific)
 */
fun Analytics.trackNOCInteraction(nocCode: String, nocTitle: String, action: String) {
    val eventName = when (action) {
        "search" -> AnalyticsEvents.NOC_CODE_SEARCHED
        "select" -> AnalyticsEvents.NOC_CODE_SELECTED
        else -> AnalyticsEvents.NOC_CODE_SEARCHED
    }
    
    trackEvent(
        eventName,
        mapOf(
            AnalyticsParams.NOC_CODE to nocCode,
            AnalyticsParams.NOC_TITLE to nocTitle
        )
    )
}

/**
 * Track sync operations
 */
fun Analytics.trackSync(type: String, success: Boolean, itemsSynced: Int, durationMs: Long) {
    val eventName = if (success) AnalyticsEvents.SYNC_COMPLETED else AnalyticsEvents.SYNC_FAILED
    
    trackEvent(
        eventName,
        mapOf(
            AnalyticsParams.SYNC_TYPE to type,
            AnalyticsParams.SUCCESS to success,
            AnalyticsParams.ITEMS_SYNCED to itemsSynced,
            AnalyticsParams.DURATION_MS to durationMs
        )
    )
}
