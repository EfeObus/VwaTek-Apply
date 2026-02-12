package com.vwatek.apply.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Application Performance Monitoring (APM) configuration for VwaTek Apply Backend
 * 
 * Features:
 * - Prometheus metrics endpoint for GCP Cloud Monitoring integration
 * - JVM metrics (memory, GC, threads, classloader)
 * - HTTP request metrics (latency, count, errors)
 * - Custom business metrics (AI requests, resume processing, user activity)
 * - Health check endpoint
 */

// Global Prometheus registry for access across the application
lateinit var appMeterRegistry: PrometheusMeterRegistry
    private set

// Custom metrics for business operations
object VwaTekMetrics {
    // AI Operations
    lateinit var aiRequestsTotal: Counter
        private set
    lateinit var aiRequestsSuccess: Counter
        private set
    lateinit var aiRequestsError: Counter
        private set
    lateinit var aiRequestDuration: Timer
        private set
    
    // Resume Operations
    lateinit var resumeUploadsTotal: Counter
        private set
    lateinit var resumeParsingSuccess: Counter
        private set
    lateinit var resumeParsingError: Counter
        private set
    lateinit var resumeParsingDuration: Timer
        private set
    
    // User Operations
    lateinit var userSignupsTotal: Counter
        private set
    lateinit var userLoginsTotal: Counter
        private set
    lateinit var activeSessionsGauge: Counter
        private set
    
    // Sync Operations
    lateinit var syncOperationsTotal: Counter
        private set
    lateinit var syncOperationsSuccess: Counter
        private set
    lateinit var syncOperationsError: Counter
        private set
    lateinit var syncDuration: Timer
        private set
    
    // Database Operations
    lateinit var dbQueryDuration: Timer
        private set
    lateinit var dbConnectionErrors: Counter
        private set
    
    fun initialize(registry: PrometheusMeterRegistry) {
        // AI Metrics
        aiRequestsTotal = Counter.builder("vwatek_ai_requests_total")
            .description("Total number of AI API requests")
            .tag("service", "gemini")
            .register(registry)
        
        aiRequestsSuccess = Counter.builder("vwatek_ai_requests_success")
            .description("Number of successful AI API requests")
            .tag("service", "gemini")
            .register(registry)
        
        aiRequestsError = Counter.builder("vwatek_ai_requests_error")
            .description("Number of failed AI API requests")
            .tag("service", "gemini")
            .register(registry)
        
        aiRequestDuration = Timer.builder("vwatek_ai_request_duration")
            .description("Duration of AI API requests")
            .tag("service", "gemini")
            .publishPercentiles(0.5, 0.75, 0.95, 0.99)
            .register(registry)
        
        // Resume Metrics
        resumeUploadsTotal = Counter.builder("vwatek_resume_uploads_total")
            .description("Total number of resume uploads")
            .register(registry)
        
        resumeParsingSuccess = Counter.builder("vwatek_resume_parsing_success")
            .description("Number of successful resume parsing operations")
            .register(registry)
        
        resumeParsingError = Counter.builder("vwatek_resume_parsing_error")
            .description("Number of failed resume parsing operations")
            .register(registry)
        
        resumeParsingDuration = Timer.builder("vwatek_resume_parsing_duration")
            .description("Duration of resume parsing operations")
            .publishPercentiles(0.5, 0.75, 0.95, 0.99)
            .register(registry)
        
        // User Metrics
        userSignupsTotal = Counter.builder("vwatek_user_signups_total")
            .description("Total number of user signups")
            .register(registry)
        
        userLoginsTotal = Counter.builder("vwatek_user_logins_total")
            .description("Total number of user logins")
            .register(registry)
        
        activeSessionsGauge = Counter.builder("vwatek_active_sessions")
            .description("Number of active user sessions")
            .register(registry)
        
        // Sync Metrics
        syncOperationsTotal = Counter.builder("vwatek_sync_operations_total")
            .description("Total number of sync operations")
            .register(registry)
        
        syncOperationsSuccess = Counter.builder("vwatek_sync_operations_success")
            .description("Number of successful sync operations")
            .register(registry)
        
        syncOperationsError = Counter.builder("vwatek_sync_operations_error")
            .description("Number of failed sync operations")
            .register(registry)
        
        syncDuration = Timer.builder("vwatek_sync_duration")
            .description("Duration of sync operations")
            .publishPercentiles(0.5, 0.75, 0.95, 0.99)
            .register(registry)
        
        // Database Metrics
        dbQueryDuration = Timer.builder("vwatek_db_query_duration")
            .description("Duration of database queries")
            .publishPercentiles(0.5, 0.75, 0.95, 0.99)
            .register(registry)
        
        dbConnectionErrors = Counter.builder("vwatek_db_connection_errors")
            .description("Number of database connection errors")
            .register(registry)
    }
}

/**
 * Configure Micrometer metrics with Prometheus registry
 */
fun Application.configureMonitoring() {
    // Create Prometheus registry
    appMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    
    // Bind JVM metrics
    ClassLoaderMetrics().bindTo(appMeterRegistry)
    JvmMemoryMetrics().bindTo(appMeterRegistry)
    JvmGcMetrics().bindTo(appMeterRegistry)
    JvmThreadMetrics().bindTo(appMeterRegistry)
    ProcessorMetrics().bindTo(appMeterRegistry)
    UptimeMetrics().bindTo(appMeterRegistry)
    
    // Initialize custom business metrics
    VwaTekMetrics.initialize(appMeterRegistry)
    
    // Install Micrometer plugin
    install(MicrometerMetrics) {
        registry = appMeterRegistry
        
        // Configure distribution statistics for latency histograms
        distributionStatisticConfig = DistributionStatisticConfig.builder()
            .percentilesHistogram(true)
            .percentiles(0.5, 0.75, 0.95, 0.99)
            .serviceLevelObjectives(
                Duration.ofMillis(100).toNanos().toDouble(),
                Duration.ofMillis(250).toNanos().toDouble(),
                Duration.ofMillis(500).toNanos().toDouble(),
                Duration.ofSeconds(1).toNanos().toDouble(),
                Duration.ofSeconds(5).toNanos().toDouble()
            )
            .minimumExpectedValue(Duration.ofMillis(1).toNanos().toDouble())
            .maximumExpectedValue(Duration.ofSeconds(30).toNanos().toDouble())
            .build()
        
        // Add common tags for all metrics
        meterBinders = listOf()
        
        // Custom timer configuration
        timers { call, _ ->
            tag("method", call.request.httpMethod.value)
            tag("route", call.request.path())
            tag("status", call.response.status()?.value?.toString() ?: "unknown")
        }
    }
    
    log.info("APM Monitoring configured with Prometheus metrics")
}

/**
 * Configure monitoring and health check routes
 */
fun Application.configureMonitoringRoutes() {
    routing {
        // Prometheus metrics endpoint (for GCP Cloud Monitoring scraping)
        get("/metrics") {
            call.respondText(
                appMeterRegistry.scrape(),
                ContentType.parse("text/plain; version=0.0.4; charset=utf-8")
            )
        }
        
        // Health check endpoint (for Kubernetes/Cloud Run health probes)
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf(
                "status" to "healthy",
                "service" to "vwatek-apply-backend",
                "timestamp" to System.currentTimeMillis().toString()
            ))
        }
        
        // Liveness probe (basic check that the service is running)
        get("/health/live") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "alive"))
        }
        
        // Readiness probe (check that the service can handle requests)
        get("/health/ready") {
            // TODO: Add database connectivity check
            val isReady = true // Add actual readiness checks
            
            if (isReady) {
                call.respond(HttpStatusCode.OK, mapOf(
                    "status" to "ready",
                    "checks" to "database:ok"
                ))
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf(
                    "status" to "not_ready"
                ))
            }
        }
    }
}

/**
 * Helper functions for recording metrics in service code
 */
object MetricsHelper {
    /**
     * Record an AI request with timing
     */
    inline fun <T> recordAIRequest(block: () -> T): T {
        VwaTekMetrics.aiRequestsTotal.increment()
        val startTime = System.nanoTime()
        
        return try {
            val result = block()
            VwaTekMetrics.aiRequestsSuccess.increment()
            result
        } catch (e: Exception) {
            VwaTekMetrics.aiRequestsError.increment()
            throw e
        } finally {
            val duration = System.nanoTime() - startTime
            VwaTekMetrics.aiRequestDuration.record(duration, TimeUnit.NANOSECONDS)
        }
    }
    
    /**
     * Record a resume parsing operation with timing
     */
    inline fun <T> recordResumeParsing(block: () -> T): T {
        VwaTekMetrics.resumeUploadsTotal.increment()
        val startTime = System.nanoTime()
        
        return try {
            val result = block()
            VwaTekMetrics.resumeParsingSuccess.increment()
            result
        } catch (e: Exception) {
            VwaTekMetrics.resumeParsingError.increment()
            throw e
        } finally {
            val duration = System.nanoTime() - startTime
            VwaTekMetrics.resumeParsingDuration.record(duration, TimeUnit.NANOSECONDS)
        }
    }
    
    /**
     * Record a sync operation with timing
     */
    inline fun <T> recordSyncOperation(block: () -> T): T {
        VwaTekMetrics.syncOperationsTotal.increment()
        val startTime = System.nanoTime()
        
        return try {
            val result = block()
            VwaTekMetrics.syncOperationsSuccess.increment()
            result
        } catch (e: Exception) {
            VwaTekMetrics.syncOperationsError.increment()
            throw e
        } finally {
            val duration = System.nanoTime() - startTime
            VwaTekMetrics.syncDuration.record(duration, TimeUnit.NANOSECONDS)
        }
    }
    
    /**
     * Record a database query with timing
     */
    inline fun <T> recordDbQuery(block: () -> T): T {
        val startTime = System.nanoTime()
        
        return try {
            block()
        } catch (e: Exception) {
            VwaTekMetrics.dbConnectionErrors.increment()
            throw e
        } finally {
            val duration = System.nanoTime() - startTime
            VwaTekMetrics.dbQueryDuration.record(duration, TimeUnit.NANOSECONDS)
        }
    }
    
    /**
     * Record a user signup
     */
    fun recordUserSignup() {
        VwaTekMetrics.userSignupsTotal.increment()
    }
    
    /**
     * Record a user login
     */
    fun recordUserLogin() {
        VwaTekMetrics.userLoginsTotal.increment()
    }
}
