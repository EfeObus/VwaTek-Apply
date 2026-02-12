package com.vwatek.apply.data.api

/**
 * Centralized API configuration for VwaTek Apply
 * 
 * Supports Canadian region deployment (northamerica-northeast1)
 */
object ApiConfig {
    
    /**
     * Current environment
     */
    enum class Environment {
        DEVELOPMENT,
        STAGING,
        PRODUCTION
    }
    
    // Default to production
    var currentEnvironment: Environment = Environment.PRODUCTION
        private set
    
    /**
     * Configure the environment
     */
    fun configure(environment: Environment) {
        currentEnvironment = environment
    }
    
    /**
     * Production API URL (Canadian region - Montreal)
     */
    private const val PRODUCTION_BASE_URL = "https://vwatek-backend-21443684777.northamerica-northeast1.run.app"
    
    /**
     * Staging API URL
     */
    private const val STAGING_BASE_URL = "https://vwatek-backend-staging.northamerica-northeast1.run.app"
    
    /**
     * Development API URL (local or us-central for dev)
     */
    private const val DEVELOPMENT_BASE_URL = "https://vwatek-backend-21443684777.us-central1.run.app"
    
    /**
     * Get the base API URL for the current environment
     */
    val baseUrl: String
        get() = when (currentEnvironment) {
            Environment.PRODUCTION -> PRODUCTION_BASE_URL
            Environment.STAGING -> STAGING_BASE_URL
            Environment.DEVELOPMENT -> DEVELOPMENT_BASE_URL
        }
    
    /**
     * Full API v1 URL
     */
    val apiV1Url: String
        get() = "$baseUrl/api/v1"
    
    /**
     * Sync API URL
     */
    val syncApiUrl: String
        get() = "$baseUrl/api/sync"
    
    /**
     * Privacy API URL
     */
    val privacyApiUrl: String
        get() = "$baseUrl/api/privacy"
    
    /**
     * Health check URL
     */
    val healthUrl: String
        get() = "$baseUrl/health"
    
    /**
     * Metrics URL
     */
    val metricsUrl: String
        get() = "$baseUrl/metrics"
}
