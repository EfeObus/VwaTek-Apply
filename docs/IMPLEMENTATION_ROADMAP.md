# VwaTek Apply - Implementation Roadmap

## Canadian Market Dominance Strategy

**Document Version:** 1.5  
**Created:** February 11, 2026  
**Last Updated:** February 2026  
**Target Completion:** Q4 2026  
**Total Estimated Duration:** 9-12 months

---

## Implementation Progress

### Phase 1: Foundation & Infrastructure - ✅ COMPLETED & AUDITED

| Task | Status | Files Created/Modified |
|------|--------|----------------------|
| Firebase Crashlytics (Android) | ✅ Complete | `androidApp/build.gradle.kts`, `VwaTekApplication.kt`, `google-services.json.template` |
| Firebase Crashlytics (iOS) | ✅ Complete | `iosApp/project.yml`, `VwaTekApplyApp.swift`, `GoogleService-Info.plist.template` |
| Sentry Web Error Tracking | ✅ Complete | `webApp/.../monitoring/SentryConfig.kt`, `Main.kt` |
| Shared Analytics Framework | ✅ Complete | `shared/.../analytics/Analytics.kt`, Platform implementations |
| Backend APM/Monitoring | ✅ Complete | `backend/.../plugins/Monitoring.kt`, `Application.kt` |
| Sync Database Tables | ✅ Complete | `backend/.../db/tables/SyncTables.kt` |
| Sync Engine (Shared) | ✅ Complete | `shared/.../sync/SyncModels.kt`, `SyncEngine.kt` |
| Sync API Routes | ✅ Complete | `backend/.../routes/SyncRoutes.kt` |
| Network Monitor | ✅ Complete | `shared/.../network/NetworkMonitor.kt`, Platform implementations |
| Terraform Canadian Region | ✅ Complete | `infrastructure/terraform/main.tf` (Montreal region config) |
| PIPEDA Consent System | ✅ Complete | `shared/.../privacy/ConsentManager.kt`, `backend/.../db/tables/PrivacyTables.kt` |
| Data Export/Deletion APIs | ✅ Complete | `backend/.../routes/PrivacyRoutes.kt` |

#### Phase 1 Audit Fixes (February 11, 2026)

| Issue | Status | Resolution |
|-------|--------|------------|
| Missing API Clients | ✅ Fixed | Created `SyncApiClient.kt`, `PrivacyApiClient.kt` with DTOs matching backend |
| Missing SyncEngineFactory | ✅ Fixed | Created `actual` implementations for Android/iOS/JS |
| Missing ConsentManagerFactory | ✅ Fixed | Created `actual` implementations for Android/iOS/JS |
| Hardcoded API URLs | ✅ Fixed | Created `ApiConfig.kt` for centralized URL management |
| URLs pointing to us-central1 | ✅ Fixed | Now uses Canadian region (northamerica-northeast1) via ApiConfig |
| Missing platform expect/actual | ✅ Fixed | Added `generateSyncOperationId()`, `currentTimeMillis()` implementations |
| Android Auth local-only | ✅ Fixed | Updated `AndroidAuthRepository.kt` to call backend API like iOS/Web |

**New Files Created in Audit:**
- `shared/src/commonMain/kotlin/com/vwatek/apply/data/api/ApiConfig.kt` - Centralized API configuration
- `shared/src/commonMain/kotlin/com/vwatek/apply/data/api/SyncApiClient.kt` - Sync API client
- `shared/src/commonMain/kotlin/com/vwatek/apply/data/api/PrivacyApiClient.kt` - Privacy API client
- `shared/src/androidMain/kotlin/com/vwatek/apply/sync/SyncEngine.android.kt` - Android sync implementation
- `shared/src/iosMain/kotlin/com/vwatek/apply/sync/SyncEngine.ios.kt` - iOS sync implementation
- `shared/src/jsMain/kotlin/com/vwatek/apply/sync/SyncEngine.js.kt` - Web sync implementation
- `shared/src/androidMain/kotlin/com/vwatek/apply/privacy/ConsentManager.android.kt` - Android consent implementation
- `shared/src/iosMain/kotlin/com/vwatek/apply/privacy/ConsentManager.ios.kt` - iOS consent implementation
- `shared/src/jsMain/kotlin/com/vwatek/apply/privacy/ConsentManager.js.kt` - Web consent implementation

**Files Modified in Route Audit:**
- `shared/src/androidMain/kotlin/com/vwatek/apply/data/repository/AndroidAuthRepository.kt` - Now calls backend `/api/v1/auth/*` endpoints

### Phase 2: Core Feature Expansion - ✅ COMPLETED & AUDITED

| Task | Status | Files Created/Modified |
|------|--------|----------------------|
| Job Tracker Database Tables | ✅ Complete | `backend/.../db/tables/JobTrackerTables.kt` |
| Job Application Models | ✅ Complete | `shared/.../domain/model/JobApplication.kt` |
| Job Tracker API Routes | ✅ Complete | `backend/.../routes/JobTrackerRoutes.kt` |
| TrackerViewModel (Shared) | ✅ Complete | `shared/.../presentation/tracker/TrackerViewModel.kt`, `JobTrackerApiClient.kt` |
| Android Kanban UI | ✅ Complete | `androidApp/.../ui/screens/TrackerScreen.kt` |
| iOS Tracker Views | ✅ Complete | `iosApp/.../Views/TrackerView.swift`, `TrackerViewModelWrapper.swift` |
| Web Tracker Components | ✅ Complete | `webApp/.../ui/screens/TrackerScreen.kt` |
| Notification System | ✅ Complete | `shared/.../domain/model/Notification.kt`, `backend/.../routes/NotificationRoutes.kt`, `backend/.../db/tables/NotificationTables.kt` |
| Chrome Extension Base | ✅ Complete | `chromeExtension/manifest.json`, `popup/*`, `background/*`, `content/*`, `lib/*`, `options/*` |

**Key Features Implemented:**
- Full Kanban board UI across all platforms (Android, iOS, Web) with drag-and-drop support
- 13 application statuses: SAVED → APPLIED → INTERVIEW stages → OFFER → ACCEPTED/REJECTED
- Canadian-focused features: Province selection (13 provinces/territories), NOC codes, LMIA/work permit tracking
- Job source tracking: LinkedIn, Indeed, Glassdoor, Job Bank Canada, Monster, Workday, etc.
- Comprehensive tracker statistics: Application counts, interview rates, offer rates, response times
- Multiple view types: Kanban, List, and Calendar views
- Filtering by status, source, province, work model, and search
- Note-taking and reminder system with notifications
- Chrome Extension with content scripts for LinkedIn, Indeed, Glassdoor, and Job Bank Canada
- Push notification infrastructure with device token management

#### Phase 2 Audit Fixes (February 11, 2026)

| Issue | Status | Resolution |
|-------|--------|------------|
| Missing TrackerUseCases | ✅ Fixed | Created `TrackerUseCases.kt` with 10 use case implementations |
| Missing DI Wiring | ✅ Fixed | Updated `Modules.kt` with JobTrackerApiClient, use cases, TrackerViewModel |
| iOS ViewModel Access | ✅ Fixed | Updated `KoinHelper.kt` with `getTrackerViewModel()` accessor |
| Chrome Extension API Mismatch | ✅ Fixed | Updated `chromeExtension/lib/api.js` with correct `/api/v1/jobs` endpoints |
| Backend Missing Endpoint | ✅ Fixed | Added `GET /api/v1/jobs/reminders/upcoming` to `JobTrackerRoutes.kt` |
| DTO Mapper Issues | ✅ Fixed | Fixed enum-to-String conversions, added missing constructor parameters |

**New Files Created in Phase 2 Audit:**
- `shared/src/commonMain/kotlin/com/vwatek/apply/domain/usecase/tracker/TrackerUseCases.kt` - All 10 use case implementations

**Files Modified in Phase 2 Audit:**
- `shared/src/commonMain/kotlin/com/vwatek/apply/di/Modules.kt` - Added tracker DI bindings
- `shared/src/iosMain/kotlin/com/vwatek/apply/di/KoinHelper.kt` - Exposed TrackerViewModel for Swift
- `chromeExtension/lib/api.js` - Fixed API endpoint routes to match backend
- `backend/src/main/kotlin/com/vwatek/apply/routes/JobTrackerRoutes.kt` - Added upcoming reminders endpoint

**Platform Parity Status (Phase 2 Tracker):**

| Feature | Web | Android | iOS |
|---------|-----|---------|-----|
| TrackerScreen | ✅ | ✅ | ✅ |
| Kanban/List/Calendar Views | ✅ | ✅ | ✅ |
| Stats Bar | ✅ | ✅ | ✅ |
| Add Application | ✅ | ✅ | ✅ |
| Filter (status/source/province) | ✅ | ✅ | ✅ |
| Application Detail View | ✅ | ✅ | ✅ |
| Update Status | ✅ | ✅ | ✅ |
| Add Note | ✅ | ✅ | ✅ |
| Add Reminder | - | ✅ | ✅ |
| Add Interview | - | - | ✅* |
| Delete Application | ✅ | ✅ | ✅ |

*Note: AddReminder UI for Web and AddInterview UI for Web/Android are available through the shared ViewModel but UI components need to be wired up.

### Phase 3: Canadian Market Differentiation - ✅ COMPLETED

### Phase 4: Premium & Monetization - ✅ COMPLETED

| Task | Status | Files Created/Modified |
|------|--------|----------------------|
| Subscription Domain Models | ✅ Complete | `shared/.../domain/model/Subscription.kt` |
| Salary Intelligence Models | ✅ Complete | `shared/.../domain/model/SalaryModels.kt` |
| Enterprise/Org Models | ✅ Complete | `shared/.../domain/model/EnterpriseModels.kt` |
| LinkedIn Optimizer Models | ✅ Complete | `shared/.../domain/model/LinkedInModels.kt` |
| Subscription DB Tables | ✅ Complete | `backend/.../db/tables/SubscriptionTables.kt` |
| Salary Data DB Tables | ✅ Complete | `backend/.../db/tables/SalaryDataTables.kt` |
| Stripe Payment Service | ✅ Complete | `backend/.../services/StripeService.kt` |
| Subscription API Routes | ✅ Complete | `backend/.../routes/SubscriptionRoutes.kt` |
| Salary API Routes | ✅ Complete | `backend/.../routes/SalaryRoutes.kt` |
| Subscription API Client | ✅ Complete | `shared/.../data/api/SubscriptionApiClient.kt` |
| Salary API Client | ✅ Complete | `shared/.../data/api/SalaryApiClient.kt` |
| Subscription Use Cases | ✅ Complete | `shared/.../domain/usecase/subscription/SubscriptionUseCases.kt` |
| Salary Intelligence Use Cases | ✅ Complete | `shared/.../domain/usecase/salary/SalaryIntelligenceUseCases.kt` |
| Android Subscription UI | ✅ Complete | `androidApp/.../ui/screens/SubscriptionScreen.kt` |
| Android Paywall UI | ✅ Complete | `androidApp/.../ui/screens/PaywallScreen.kt` |
| Android Salary Insights UI | ✅ Complete | `androidApp/.../ui/screens/SalaryInsightsScreen.kt` |
| iOS Subscription UI | ✅ Complete | `iosApp/.../Views/SubscriptionView.swift` |
| iOS Paywall UI | ✅ Complete | `iosApp/.../Views/PaywallView.swift` |
| iOS Salary Insights UI | ✅ Complete | `iosApp/.../Views/SalaryInsightsView.swift` |
| Web Subscription UI | ✅ Complete | `webApp/.../ui/screens/SubscriptionScreen.kt` |
| Web Paywall UI | ✅ Complete | `webApp/.../ui/screens/PaywallScreen.kt` |
| Web Salary Insights UI | ✅ Complete | `webApp/.../ui/screens/SalaryInsightsScreen.kt` |

**Key Features Implemented:**
- Three-tier subscription system: FREE, PRO ($14.99/mo), PREMIUM ($29.99/mo)
- Feature gating with FeatureLimits per tier (resumes, cover letters, AI enhancements)
- Stripe integration for web payments with webhook handling
- Apple In-App Purchase integration for iOS
- Google Play Billing integration for Android
- Salary insights with market data and percentile analysis
- AI negotiation coach for offer evaluation
- Dynamic paywall components with feature-gated access
- Billing period toggle (monthly/yearly with ~17% savings)

### Phase 5: Scale & Enterprise - ✅ COMPLETED

| Task | Status | Files Created/Modified |
|------|--------|----------------------|
| Enterprise DB Tables | ✅ Complete | `backend/.../db/tables/EnterpriseTables.kt` |
| Organization Management | ✅ Complete | 9 tables for orgs, members, SSO, invitations |
| SSO Configuration | ✅ Complete | SAML, OIDC, Azure AD, Okta, Google Workspace support |
| LinkedIn Optimizer Use Cases | ✅ Complete | `shared/.../domain/usecase/linkedin/LinkedInOptimizerUseCases.kt` |

**Key Features Implemented:**
- Organization entities with settings and templates
- Role-based access: OWNER, ADMIN, MANAGER, MEMBER
- SSO provider configuration (SAML, OIDC, Azure AD, Okta, Google Workspace)
- LinkedIn profile analysis with section scoring
- LinkedIn content optimization suggestions
- Headline generation and improvement recommendations

**Legal & Compliance Documents Created:**
- `docs/TERMS_OF_SERVICE.md` - General Terms of Service
- `docs/PRIVACY_POLICY.md` - PIPEDA-compliant Privacy Policy
- `docs/SUBSCRIPTION_TERMS.md` - Detailed subscription terms
- `docs/REFUND_POLICY.md` - 30-day money-back guarantee policy
- `docs/CANCELLATION_POLICY.md` - Self-service cancellation procedures
- `docs/AUTO_RENEWAL_DISCLOSURE.md` - Canadian consumer law compliant disclosure

---

## Table of Contents

1. [Overview](#overview)
2. [Phase 1: Foundation & Infrastructure](#phase-1-foundation--infrastructure)
3. [Phase 2: Core Feature Expansion](#phase-2-core-feature-expansion)
4. [Phase 3: Canadian Market Differentiation](#phase-3-canadian-market-differentiation)
5. [Phase 4: Premium & Monetization](#phase-4-premium--monetization)
6. [Phase 5: Scale & Enterprise](#phase-5-scale--enterprise)
7. [Technical Specifications](#technical-specifications)
8. [Success Metrics](#success-metrics)

---

## Overview

### Vision
Transform VwaTek Apply from a generic AI career assistant into **Canada's premier job search platform**, uniquely tailored for the Canadian job market, immigration pathways, and bilingual workforce requirements.

### Strategic Pillars
1. **Canadian-First** - NOC integration, Job Bank, bilingual support
2. **End-to-End** - From resume creation to job offer acceptance tracking
3. **Data-Driven** - Analytics that help users optimize their job search
4. **Privacy-Focused** - PIPEDA/Quebec Law 25 compliant, Canadian data residency

---

## Phase 1: Foundation & Infrastructure

**Duration:** 6-8 weeks  
**Priority:** CRITICAL  
**Theme:** "Build the foundation for scale"

### 1.1 Observability & Monitoring

#### 1.1.1 Crash Reporting Integration
**Effort:** 3 days  
**Owner:** Mobile/Platform Team

**Android Implementation:**
```kotlin
// androidApp/build.gradle.kts
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
}

// androidApp/src/androidMain/kotlin/com/vwatek/apply/android/VwaTekApplication.kt
class VwaTekApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }
}
```

**iOS Implementation:**
```swift
// iosApp/iosApp/VwaTekApplyApp.swift
import FirebaseCrashlytics

@main
struct VwaTekApplyApp: App {
    init() {
        FirebaseApp.configure()
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(true)
    }
}
```

**Web Implementation:**
```kotlin
// webApp/src/jsMain/kotlin/com/vwatek/apply/monitoring/SentryConfig.kt
external fun Sentry_init(options: dynamic)

fun initSentry() {
    Sentry_init(js("""
        {
            dsn: "${BuildKonfig.SENTRY_DSN}",
            environment: "${BuildKonfig.ENVIRONMENT}",
            tracesSampleRate: 0.1
        }
    """))
}
```

**Acceptance Criteria:**
- [ ] Crashes automatically reported within 5 minutes
- [ ] Stack traces include device/OS info
- [ ] Non-fatal errors logged for debugging
- [ ] Dashboard accessible to dev team

---

#### 1.1.2 Analytics Framework
**Effort:** 5 days  
**Owner:** Platform Team

**Shared Analytics Interface:**
```kotlin
// shared/src/commonMain/kotlin/com/vwatek/apply/analytics/Analytics.kt
interface AnalyticsTracker {
    fun trackEvent(event: AnalyticsEvent)
    fun setUserProperty(key: String, value: String)
    fun setUserId(userId: String?)
}

sealed class AnalyticsEvent(val name: String, val params: Map<String, Any> = emptyMap()) {
    // Resume Events
    data class ResumeCreated(val industry: String?) : AnalyticsEvent(
        "resume_created", mapOf("industry" to (industry ?: "unknown"))
    )
    data class ResumeAnalyzed(val matchScore: Int) : AnalyticsEvent(
        "resume_analyzed", mapOf("match_score" to matchScore)
    )
    
    // Cover Letter Events
    data class CoverLetterGenerated(val tone: String) : AnalyticsEvent(
        "cover_letter_generated", mapOf("tone" to tone)
    )
    
    // Interview Events
    data class InterviewSessionStarted(val jobTitle: String) : AnalyticsEvent(
        "interview_session_started", mapOf("job_title" to jobTitle)
    )
    
    // Job Tracker Events (Phase 2)
    data class JobApplicationAdded(val source: String, val status: String) : AnalyticsEvent(
        "job_application_added", mapOf("source" to source, "status" to status)
    )
    data class JobStatusChanged(val fromStatus: String, val toStatus: String) : AnalyticsEvent(
        "job_status_changed", mapOf("from" to fromStatus, "to" to toStatus)
    )
}
```

**Platform Implementations:**
```kotlin
// androidMain - Firebase Analytics
actual class AnalyticsTrackerImpl(private val firebaseAnalytics: FirebaseAnalytics) : AnalyticsTracker {
    actual override fun trackEvent(event: AnalyticsEvent) {
        firebaseAnalytics.logEvent(event.name, bundleOf(*event.params.toList().toTypedArray()))
    }
}

// iosMain - Firebase Analytics via Kotlin
actual class AnalyticsTrackerImpl : AnalyticsTracker {
    actual override fun trackEvent(event: AnalyticsEvent) {
        Analytics.logEvent(event.name, parameters: event.params)
    }
}

// jsMain - Mixpanel/Amplitude
actual class AnalyticsTrackerImpl : AnalyticsTracker {
    actual override fun trackEvent(event: AnalyticsEvent) {
        js("mixpanel.track(event.name, event.params)")
    }
}
```

**Acceptance Criteria:**
- [ ] All key user actions tracked
- [ ] User ID linked across sessions (if authenticated)
- [ ] Dashboard with funnel visualization
- [ ] Privacy-compliant (anonymized by default)

---

#### 1.1.3 Application Performance Monitoring (APM)
**Effort:** 3 days  
**Owner:** Backend Team

**Backend APM Setup:**
```kotlin
// backend/src/main/kotlin/com/vwatek/apply/plugins/Monitoring.kt
fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/api") }
        mdc("requestId") { call.request.header("X-Request-ID") ?: UUID.randomUUID().toString() }
    }
    
    install(MicrometerMetrics) {
        registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        meterBinders = listOf(
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics(),
            JvmThreadMetrics()
        )
    }
    
    routing {
        get("/metrics") {
            call.respond((registry as PrometheusMeterRegistry).scrape())
        }
    }
}
```

**Cloud Monitoring Integration (Terraform):**
```hcl
# infrastructure/terraform/monitoring.tf
resource "google_monitoring_alert_policy" "high_latency" {
  display_name = "VwaTek API High Latency"
  combiner     = "OR"
  
  conditions {
    display_name = "API latency > 2s"
    condition_threshold {
      filter          = "resource.type=\"cloud_run_revision\" AND metric.type=\"run.googleapis.com/request_latencies\""
      duration        = "60s"
      comparison      = "COMPARISON_GT"
      threshold_value = 2000
      aggregations {
        alignment_period   = "60s"
        per_series_aligner = "ALIGN_PERCENTILE_99"
      }
    }
  }
  
  notification_channels = [google_monitoring_notification_channel.email.id]
}

resource "google_monitoring_dashboard" "vwatek_dashboard" {
  dashboard_json = jsonencode({
    displayName = "VwaTek Apply Dashboard"
    gridLayout = {
      widgets = [
        {
          title = "Request Count"
          xyChart = {
            dataSets = [{
              timeSeriesQuery = {
                timeSeriesFilter = {
                  filter = "resource.type=\"cloud_run_revision\" metric.type=\"run.googleapis.com/request_count\""
                }
              }
            }]
          }
        }
      ]
    }
  })
}
```

---

### 1.2 Cloud Sync Architecture

#### 1.2.1 Database Schema Updates
**Effort:** 5 days  
**Owner:** Backend Team

**New Tables for Sync:**
```kotlin
// backend/src/main/kotlin/com/vwatek/apply/db/tables/SyncTables.kt

// Device registration for sync
object DevicesTable : Table("devices") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val deviceType = varchar("device_type", 20) // IOS, ANDROID, WEB
    val deviceName = varchar("device_name", 255).nullable()
    val pushToken = text("push_token").nullable()
    val lastSyncAt = timestamp("last_sync_at").nullable()
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
}

// Sync log for conflict resolution
object SyncLogTable : Table("sync_log") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val entityType = varchar("entity_type", 50) // RESUME, COVER_LETTER, JOB_APPLICATION
    val entityId = varchar("entity_id", 36)
    val operation = varchar("operation", 20) // CREATE, UPDATE, DELETE
    val clientTimestamp = timestamp("client_timestamp")
    val serverTimestamp = timestamp("server_timestamp")
    val deviceId = varchar("device_id", 36).references(DevicesTable.id)
    val checksum = varchar("checksum", 64) // For conflict detection
    
    override val primaryKey = PrimaryKey(id)
}

// Add sync columns to existing tables
// ALTER TABLE resumes ADD COLUMN sync_status VARCHAR(20) DEFAULT 'SYNCED';
// ALTER TABLE resumes ADD COLUMN local_updated_at TIMESTAMP;
// ALTER TABLE resumes ADD COLUMN server_updated_at TIMESTAMP;
// ALTER TABLE resumes ADD COLUMN checksum VARCHAR(64);
```

**Sync Status Enum:**
```kotlin
// shared/src/commonMain/kotlin/com/vwatek/apply/domain/model/SyncModels.kt
enum class SyncStatus {
    SYNCED,           // In sync with server
    PENDING_UPLOAD,   // Local changes not yet uploaded
    PENDING_DOWNLOAD, // Server has newer version
    CONFLICT          // Both local and server changed
}

data class SyncableEntity<T>(
    val data: T,
    val syncStatus: SyncStatus,
    val localUpdatedAt: Instant,
    val serverUpdatedAt: Instant?,
    val checksum: String
)
```

---

#### 1.2.2 Sync Engine Implementation
**Effort:** 10 days  
**Owner:** Platform Team

**Sync Manager Interface:**
```kotlin
// shared/src/commonMain/kotlin/com/vwatek/apply/data/sync/SyncManager.kt
interface SyncManager {
    val syncState: StateFlow<SyncState>
    
    suspend fun startSync(): SyncResult
    suspend fun syncEntity(entityType: EntityType, entityId: String): SyncResult
    suspend fun resolveConflict(entityId: String, resolution: ConflictResolution): SyncResult
    fun observeSyncStatus(): Flow<SyncState>
}

data class SyncState(
    val isSyncing: Boolean = false,
    val lastSyncTime: Instant? = null,
    val pendingChanges: Int = 0,
    val conflicts: List<SyncConflict> = emptyList(),
    val error: SyncError? = null
)

sealed class SyncResult {
    object Success : SyncResult()
    data class PartialSuccess(val synced: Int, val failed: Int) : SyncResult()
    data class Conflict(val conflicts: List<SyncConflict>) : SyncResult()
    data class Error(val error: SyncError) : SyncResult()
}

data class SyncConflict(
    val entityType: EntityType,
    val entityId: String,
    val localVersion: Any,
    val serverVersion: Any,
    val localTimestamp: Instant,
    val serverTimestamp: Instant
)

enum class ConflictResolution {
    KEEP_LOCAL,
    KEEP_SERVER,
    MERGE // For compatible changes
}
```

**Sync Engine Implementation:**
```kotlin
// shared/src/commonMain/kotlin/com/vwatek/apply/data/sync/SyncManagerImpl.kt
class SyncManagerImpl(
    private val resumeRepository: ResumeRepository,
    private val coverLetterRepository: CoverLetterRepository,
    private val syncApi: SyncApiService,
    private val networkMonitor: NetworkMonitor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : SyncManager {
    
    private val _syncState = MutableStateFlow(SyncState())
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    override suspend fun startSync(): SyncResult = withContext(dispatcher) {
        if (!networkMonitor.isConnected()) {
            return@withContext SyncResult.Error(SyncError.NoNetwork)
        }
        
        _syncState.update { it.copy(isSyncing = true) }
        
        try {
            // 1. Pull server changes since last sync
            val serverChanges = syncApi.getChangesSince(_syncState.value.lastSyncTime)
            
            // 2. Apply non-conflicting server changes
            val conflicts = mutableListOf<SyncConflict>()
            serverChanges.forEach { change ->
                val result = applyServerChange(change)
                if (result is ApplyResult.Conflict) {
                    conflicts.add(result.conflict)
                }
            }
            
            // 3. Push local changes
            val localChanges = getLocalPendingChanges()
            localChanges.forEach { change ->
                val result = pushLocalChange(change)
                if (result is PushResult.Conflict) {
                    conflicts.add(result.conflict)
                }
            }
            
            // 4. Update sync state
            _syncState.update { 
                it.copy(
                    isSyncing = false,
                    lastSyncTime = Clock.System.now(),
                    pendingChanges = 0,
                    conflicts = conflicts
                )
            }
            
            if (conflicts.isNotEmpty()) {
                SyncResult.Conflict(conflicts)
            } else {
                SyncResult.Success
            }
        } catch (e: Exception) {
            _syncState.update { it.copy(isSyncing = false, error = SyncError.Unknown(e.message)) }
            SyncResult.Error(SyncError.Unknown(e.message))
        }
    }
    
    private suspend fun applyServerChange(change: ServerChange): ApplyResult {
        val localEntity = getLocalEntity(change.entityType, change.entityId)
        
        // No local version - just apply
        if (localEntity == null) {
            saveServerEntity(change)
            return ApplyResult.Applied
        }
        
        // Check for conflict
        if (localEntity.syncStatus == SyncStatus.PENDING_UPLOAD) {
            // Both changed - conflict!
            return ApplyResult.Conflict(
                SyncConflict(
                    entityType = change.entityType,
                    entityId = change.entityId,
                    localVersion = localEntity.data,
                    serverVersion = change.data,
                    localTimestamp = localEntity.localUpdatedAt,
                    serverTimestamp = change.serverTimestamp
                )
            )
        }
        
        // Local hasn't changed - apply server version
        saveServerEntity(change)
        return ApplyResult.Applied
    }
}
```

**Sync API Endpoints:**
```kotlin
// backend/src/main/kotlin/com/vwatek/apply/routes/SyncRoutes.kt
fun Route.syncRoutes() {
    authenticate("jwt") {
        route("/api/v1/sync") {
            // Get changes since timestamp
            get("/changes") {
                val since = call.request.queryParameters["since"]?.let { Instant.parse(it) }
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                
                val changes = syncService.getChangesSince(userId, since)
                call.respond(SyncChangesResponse(changes = changes, serverTime = Clock.System.now()))
            }
            
            // Push local changes
            post("/push") {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val request = call.receive<SyncPushRequest>()
                
                val results = syncService.applyChanges(userId, request.changes)
                call.respond(SyncPushResponse(results = results))
            }
            
            // Resolve conflict
            post("/resolve") {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val request = call.receive<ConflictResolutionRequest>()
                
                syncService.resolveConflict(userId, request)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
```

---

#### 1.2.3 Offline-First Architecture
**Effort:** 5 days  
**Owner:** Platform Team

**Network Monitor:**
```kotlin
// shared/src/commonMain/kotlin/com/vwatek/apply/data/network/NetworkMonitor.kt
expect class NetworkMonitor {
    fun isConnected(): Boolean
    fun observeNetworkState(): Flow<NetworkState>
}

enum class NetworkState {
    CONNECTED,
    DISCONNECTED,
    METERED // For data-saving mode
}

// androidMain
actual class NetworkMonitor(private val context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    actual fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    actual fun observeNetworkState(): Flow<NetworkState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkState.CONNECTED)
            }
            override fun onLost(network: Network) {
                trySend(NetworkState.DISCONNECTED)
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
}
```

**Queue for Offline Operations:**
```kotlin
// shared/src/commonMain/kotlin/com/vwatek/apply/data/sync/OperationQueue.kt
class OperationQueue(
    private val database: VwaTekDatabase,
    private val networkMonitor: NetworkMonitor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    fun enqueue(operation: PendingOperation) {
        database.pendingOperationQueries.insert(
            id = UUID.randomUUID().toString(),
            entityType = operation.entityType.name,
            entityId = operation.entityId,
            operationType = operation.type.name,
            payload = Json.encodeToString(operation.payload),
            createdAt = Clock.System.now()
        )
        
        // Try to process immediately if online
        if (networkMonitor.isConnected()) {
            scope.launch { processQueue() }
        }
    }
    
    suspend fun processQueue() {
        val pending = database.pendingOperationQueries.selectAll().executeAsList()
        pending.forEach { operation ->
            try {
                executeOperation(operation)
                database.pendingOperationQueries.delete(operation.id)
            } catch (e: Exception) {
                // Will retry on next sync
                log.error("Failed to process operation ${operation.id}", e)
            }
        }
    }
}
```

---

### 1.3 Canadian Data Residency

#### 1.3.1 Infrastructure Migration
**Effort:** 3 days  
**Owner:** DevOps Team

**Terraform Updates for Canadian Region:**
```hcl
# infrastructure/terraform/variables.tf
variable "region" {
  description = "GCP Region"
  type        = string
  default     = "northamerica-northeast1" # Montreal
}

variable "zone" {
  description = "GCP Zone"
  type        = string
  default     = "northamerica-northeast1-a"
}

# Cloud SQL in Canadian Region
resource "google_sql_database_instance" "main_canada" {
  name             = "vwatekapply-canada"
  database_version = "MYSQL_8_0"
  region           = "northamerica-northeast1"
  
  settings {
    tier              = "db-custom-2-4096"
    availability_type = "REGIONAL" # High availability
    
    backup_configuration {
      enabled            = true
      binary_log_enabled = true
      start_time         = "03:00" # 3 AM ET
      location           = "northamerica-northeast1"
      
      backup_retention_settings {
        retained_backups = 30
      }
    }
    
    ip_configuration {
      ipv4_enabled    = false
      private_network = google_compute_network.vpc.id
    }
    
    maintenance_window {
      day          = 7 # Sunday
      hour         = 4 # 4 AM ET
      update_track = "stable"
    }
  }
  
  deletion_protection = true
}

# Cloud Run in Canadian Region
resource "google_cloud_run_service" "backend_canada" {
  name     = "vwatek-backend"
  location = "northamerica-northeast1"
  
  template {
    spec {
      containers {
        image = "gcr.io/${var.project_id}/vwatek-backend:latest"
        
        resources {
          limits = {
            cpu    = "2"
            memory = "1Gi"
          }
        }
        
        env {
          name  = "DATA_REGION"
          value = "CA"
        }
      }
    }
    
    metadata {
      annotations = {
        "autoscaling.knative.dev/minScale" = "1"
        "autoscaling.knative.dev/maxScale" = "10"
        "run.googleapis.com/cloudsql-instances" = google_sql_database_instance.main_canada.connection_name
      }
    }
  }
}
```

---

### 1.4 Privacy & Compliance Foundation

#### 1.4.1 PIPEDA Compliance Implementation
**Effort:** 5 days  
**Owner:** Full Stack Team

**Data Processing Consent:**
```kotlin
// shared/src/commonMain/kotlin/com/vwatek/apply/domain/model/ConsentModels.kt
data class UserConsent(
    val id: String,
    val userId: String,
    val consentType: ConsentType,
    val granted: Boolean,
    val grantedAt: Instant?,
    val revokedAt: Instant?,
    val version: String // Consent policy version
)

enum class ConsentType {
    DATA_PROCESSING,      // Required for service
    ANALYTICS,            // Optional
    MARKETING,            // Optional
    AI_IMPROVEMENT,       // Optional - use data to improve AI
    THIRD_PARTY_SHARING   // Optional - job board integrations
}

// backend/src/main/kotlin/com/vwatek/apply/routes/ConsentRoutes.kt
fun Route.consentRoutes() {
    authenticate("jwt") {
        route("/api/v1/consent") {
            get {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val consents = consentService.getUserConsents(userId)
                call.respond(consents)
            }
            
            post {
                val userId = call.principal<JWTPrincipal>()!!.subject!!
                val request = call.receive<UpdateConsentRequest>()
                consentService.updateConsent(userId, request.consentType, request.granted)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
```

**Data Export (Right to Access):**
```kotlin
// backend/src/main/kotlin/com/vwatek/apply/services/DataExportService.kt
class DataExportService(
    private val userRepository: UserRepository,
    private val resumeRepository: ResumeRepository,
    private val coverLetterRepository: CoverLetterRepository,
    private val interviewRepository: InterviewRepository
) {
    suspend fun exportUserData(userId: String): UserDataExport {
        val user = userRepository.getById(userId)
        val resumes = resumeRepository.getByUserId(userId)
        val coverLetters = coverLetterRepository.getByUserId(userId)
        val interviews = interviewRepository.getByUserId(userId)
        
        return UserDataExport(
            exportedAt = Clock.System.now(),
            user = user.toExportModel(),
            resumes = resumes.map { it.toExportModel() },
            coverLetters = coverLetters.map { it.toExportModel() },
            interviewSessions = interviews.map { it.toExportModel() },
            metadata = ExportMetadata(
                format = "JSON",
                version = "1.0",
                dataRetentionPolicy = "Data retained for 3 years after last activity"
            )
        )
    }
    
    suspend fun generateExportFile(userId: String): ByteArray {
        val export = exportUserData(userId)
        return Json.encodeToString(export).encodeToByteArray()
    }
}
```

**Data Deletion (Right to be Forgotten):**
```kotlin
// backend/src/main/kotlin/com/vwatek/apply/services/DataDeletionService.kt
class DataDeletionService(
    private val database: Database,
    private val storageService: StorageService
) {
    suspend fun requestDeletion(userId: String): DeletionRequest {
        // Create deletion request with 30-day grace period (PIPEDA requirement)
        val request = DeletionRequest(
            id = UUID.randomUUID().toString(),
            userId = userId,
            requestedAt = Clock.System.now(),
            scheduledDeletionAt = Clock.System.now().plus(30.days),
            status = DeletionStatus.PENDING
        )
        
        database.deletionRequestQueries.insert(request)
        
        // Send confirmation email
        emailService.sendDeletionConfirmation(userId, request)
        
        return request
    }
    
    suspend fun executeDeletion(requestId: String) {
        val request = database.deletionRequestQueries.getById(requestId).executeAsOne()
        
        transaction {
            // Delete in order respecting foreign keys
            database.interviewQuestionsQueries.deleteByUserId(request.userId)
            database.interviewSessionsQueries.deleteByUserId(request.userId)
            database.coverLettersQueries.deleteByUserId(request.userId)
            database.resumeVersionsQueries.deleteByUserId(request.userId)
            database.resumeAnalysesQueries.deleteByUserId(request.userId)
            database.resumesQueries.deleteByUserId(request.userId)
            database.syncLogQueries.deleteByUserId(request.userId)
            database.devicesQueries.deleteByUserId(request.userId)
            database.consentsQueries.deleteByUserId(request.userId)
            database.usersQueries.delete(request.userId)
            
            // Update request status
            database.deletionRequestQueries.markCompleted(requestId, Clock.System.now())
        }
        
        // Delete from storage (profile images, etc.)
        storageService.deleteUserFiles(request.userId)
    }
}
```

---

### Phase 1 Deliverables Checklist

| Deliverable | Owner | Status |
|-------------|-------|--------|
| Firebase Crashlytics (Android) | Mobile | ⬜ |
| Firebase Crashlytics (iOS) | Mobile | ⬜ |
| Sentry Integration (Web) | Web | ⬜ |
| Analytics Framework | Platform | ⬜ |
| Cloud Monitoring Dashboard | DevOps | ⬜ |
| Alert Policies | DevOps | ⬜ |
| Sync Database Schema | Backend | ⬜ |
| Sync Engine Implementation | Platform | ⬜ |
| Offline Queue System | Platform | ⬜ |
| Canadian Region Migration | DevOps | ⬜ |
| PIPEDA Consent Flows | Full Stack | ⬜ |
| Data Export API | Backend | ⬜ |
| Data Deletion API | Backend | ⬜ |

---

## Phase 2: Core Feature Expansion

**Duration:** 8-10 weeks  
**Priority:** HIGH  
**Theme:** "Table stakes for serious job seekers"

### 2.1 Job Application Tracker

#### 2.1.1 Data Model
**Effort:** 3 days  
**Owner:** Backend Team

```kotlin
// backend/src/main/kotlin/com/vwatek/apply/db/tables/JobTrackerTables.kt

object JobApplicationsTable : Table("job_applications") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val resumeId = varchar("resume_id", 36).references(ResumesTable.id).nullable()
    val coverLetterId = varchar("cover_letter_id", 36).references(CoverLettersTable.id).nullable()
    
    // Job Details
    val jobTitle = varchar("job_title", 255)
    val companyName = varchar("company_name", 255)
    val companyLogo = text("company_logo").nullable()
    val jobUrl = text("job_url").nullable()
    val jobBoardSource = varchar("job_board_source", 50).nullable() // INDEED, LINKEDIN, JOB_BANK, MANUAL
    val externalJobId = varchar("external_job_id", 100).nullable()
    
    // Location
    val city = varchar("city", 100).nullable()
    val province = varchar("province", 50).nullable() // Canadian provinces
    val isRemote = bool("is_remote").default(false)
    val isHybrid = bool("is_hybrid").default(false)
    
    // Compensation
    val salaryMin = integer("salary_min").nullable()
    val salaryMax = integer("salary_max").nullable()
    val salaryCurrency = varchar("salary_currency", 3).default("CAD")
    val salaryPeriod = varchar("salary_period", 20).nullable() // HOURLY, ANNUAL
    
    // Application Status
    val status = varchar("status", 30).default("SAVED")
    val appliedAt = timestamp("applied_at").nullable()
    val responseReceivedAt = timestamp("response_received_at").nullable()
    
    // Canadian Specific
    val nocCode = varchar("noc_code", 10).nullable()
    val requiresWorkPermit = bool("requires_work_permit").nullable()
    val isLmiaRequired = bool("is_lmia_required").nullable()
    
    // Sync
    val syncStatus = varchar("sync_status", 20).default("SYNCED")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
}

object JobApplicationStatusHistoryTable : Table("job_application_status_history") {
    val id = varchar("id", 36)
    val applicationId = varchar("application_id", 36).references(JobApplicationsTable.id)
    val fromStatus = varchar("from_status", 30).nullable()
    val toStatus = varchar("to_status", 30)
    val notes = text("notes").nullable()
    val changedAt = timestamp("changed_at")
    
    override val primaryKey = PrimaryKey(id)
}

object JobApplicationNotesTable : Table("job_application_notes") {
    val id = varchar("id", 36)
    val applicationId = varchar("application_id", 36).references(JobApplicationsTable.id)
    val content = text("content")
    val noteType = varchar("note_type", 30).default("GENERAL") // GENERAL, INTERVIEW, FOLLOW_UP
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
}

object JobApplicationRemindersTable : Table("job_application_reminders") {
    val id = varchar("id", 36)
    val applicationId = varchar("application_id", 36).references(JobApplicationsTable.id)
    val reminderType = varchar("reminder_type", 30) // FOLLOW_UP, INTERVIEW, DEADLINE
    val reminderAt = timestamp("reminder_at")
    val message = text("message").nullable()
    val isCompleted = bool("is_completed").default(false)
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
}
```

**Application Status Enum:**
```kotlin
// shared/src/commonMain/kotlin/com/vwatek/apply/domain/model/JobApplication.kt
enum class ApplicationStatus(val displayName: String, val color: Long) {
    SAVED("Saved", 0xFF9E9E9E),           // Grey - Just saved, not applied yet
    APPLIED("Applied", 0xFF2196F3),        // Blue - Application submitted
    VIEWED("Viewed", 0xFF03A9F4),          // Light Blue - Employer viewed
    PHONE_SCREEN("Phone Screen", 0xFF00BCD4), // Cyan - Initial call scheduled
    INTERVIEW("Interview", 0xFF009688),     // Teal - Interview scheduled
    ASSESSMENT("Assessment", 0xFF4CAF50),   // Green - Task/assessment stage
    FINAL_ROUND("Final Round", 0xFF8BC34A), // Light Green - Final interviews
    OFFER("Offer", 0xFFCDDC39),             // Lime - Received offer
    NEGOTIATING("Negotiating", 0xFFFFEB3B), // Yellow - Negotiating terms
    ACCEPTED("Accepted", 0xFF4CAF50),       // Green - Accepted offer
    REJECTED("Rejected", 0xFFF44336),       // Red - Rejected/declined
    WITHDRAWN("Withdrawn", 0xFF9E9E9E),     // Grey - Withdrew application
    NO_RESPONSE("No Response", 0xFFFF9800)  // Orange - No response after time
}

data class JobApplication(
    val id: String,
    val userId: String,
    val resumeId: String?,
    val coverLetterId: String?,
    val jobTitle: String,
    val companyName: String,
    val companyLogo: String?,
    val jobUrl: String?,
    val jobBoardSource: JobBoardSource?,
    val externalJobId: String?,
    val city: String?,
    val province: CanadianProvince?,
    val isRemote: Boolean,
    val isHybrid: Boolean,
    val salaryMin: Int?,
    val salaryMax: Int?,
    val salaryCurrency: String,
    val salaryPeriod: SalaryPeriod?,
    val status: ApplicationStatus,
    val appliedAt: Instant?,
    val responseReceivedAt: Instant?,
    val nocCode: String?,
    val requiresWorkPermit: Boolean?,
    val isLmiaRequired: Boolean?,
    val notes: List<ApplicationNote>,
    val reminders: List<ApplicationReminder>,
    val statusHistory: List<StatusChange>,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class CanadianProvince(val code: String, val fullName: String) {
    AB("AB", "Alberta"),
    BC("BC", "British Columbia"),
    MB("MB", "Manitoba"),
    NB("NB", "New Brunswick"),
    NL("NL", "Newfoundland and Labrador"),
    NS("NS", "Nova Scotia"),
    NT("NT", "Northwest Territories"),
    NU("NU", "Nunavut"),
    ON("ON", "Ontario"),
    PE("PE", "Prince Edward Island"),
    QC("QC", "Quebec"),
    SK("SK", "Saskatchewan"),
    YT("YT", "Yukon")
}

enum class JobBoardSource {
    INDEED, LINKEDIN, JOB_BANK, GLASSDOOR, WORKDAY, MONSTER, COMPANY_SITE, REFERRAL, MANUAL
}
```

---

#### 2.1.2 Tracker UI - Kanban Board
**Effort:** 8 days  
**Owner:** UI Team

```kotlin
// shared/src/commonMain/kotlin/com/vwatek/apply/presentation/tracker/TrackerViewModel.kt
class TrackerViewModel(
    private val getApplicationsUseCase: GetApplicationsUseCase,
    private val updateApplicationStatusUseCase: UpdateApplicationStatusUseCase,
    private val createReminderUseCase: CreateReminderUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {
    
    private val _state = MutableStateFlow(TrackerState())
    val state: StateFlow<TrackerState> = _state.asStateFlow()
    
    init {
        loadApplications()
    }
    
    fun onIntent(intent: TrackerIntent) {
        when (intent) {
            is TrackerIntent.LoadApplications -> loadApplications()
            is TrackerIntent.FilterByStatus -> filterByStatus(intent.statuses)
            is TrackerIntent.FilterByProvince -> filterByProvince(intent.province)
            is TrackerIntent.MoveToStatus -> moveApplication(intent.applicationId, intent.newStatus)
            is TrackerIntent.AddNote -> addNote(intent.applicationId, intent.note)
            is TrackerIntent.SetReminder -> setReminder(intent.applicationId, intent.reminder)
            is TrackerIntent.ChangeView -> changeView(intent.viewType)
            is TrackerIntent.Search -> search(intent.query)
        }
    }
    
    private fun loadApplications() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getApplicationsUseCase().collect { applications ->
                val grouped = applications.groupBy { it.status }
                _state.update { 
                    it.copy(
                        isLoading = false,
                        applications = applications,
                        applicationsByStatus = grouped,
                        stats = calculateStats(applications)
                    )
                }
            }
        }
    }
    
    private fun calculateStats(applications: List<JobApplication>): TrackerStats {
        val total = applications.size
        val applied = applications.count { it.status >= ApplicationStatus.APPLIED }
        val interviews = applications.count { 
            it.status in listOf(ApplicationStatus.PHONE_SCREEN, ApplicationStatus.INTERVIEW, ApplicationStatus.FINAL_ROUND)
        }
        val offers = applications.count { 
            it.status in listOf(ApplicationStatus.OFFER, ApplicationStatus.NEGOTIATING, ApplicationStatus.ACCEPTED)
        }
        val rejected = applications.count { it.status == ApplicationStatus.REJECTED }
        
        return TrackerStats(
            totalApplications = total,
            appliedCount = applied,
            interviewRate = if (applied > 0) (interviews.toFloat() / applied * 100) else 0f,
            offerRate = if (interviews > 0) (offers.toFloat() / interviews * 100) else 0f,
            rejectionCount = rejected,
            averageResponseDays = calculateAverageResponseDays(applications),
            weeklyApplications = calculateWeeklyTrend(applications)
        )
    }
}

data class TrackerState(
    val isLoading: Boolean = false,
    val applications: List<JobApplication> = emptyList(),
    val applicationsByStatus: Map<ApplicationStatus, List<JobApplication>> = emptyMap(),
    val stats: TrackerStats = TrackerStats(),
    val viewType: TrackerViewType = TrackerViewType.KANBAN,
    val filter: TrackerFilter = TrackerFilter(),
    val searchQuery: String = "",
    val error: String? = null
)

data class TrackerStats(
    val totalApplications: Int = 0,
    val appliedCount: Int = 0,
    val interviewRate: Float = 0f,
    val offerRate: Float = 0f,
    val rejectionCount: Int = 0,
    val averageResponseDays: Float = 0f,
    val weeklyApplications: List<WeeklyData> = emptyList()
)

enum class TrackerViewType {
    KANBAN, LIST, CALENDAR
}
```

**Compose UI (Android):**
```kotlin
// androidApp/src/androidMain/kotlin/com/vwatek/apply/android/ui/screens/TrackerScreen.kt
@Composable
fun TrackerScreen(
    viewModel: TrackerViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Stats Bar
        TrackerStatsBar(stats = state.stats)
        
        // View Toggle
        ViewToggle(
            currentView = state.viewType,
            onViewChange = { viewModel.onIntent(TrackerIntent.ChangeView(it)) }
        )
        
        // Search & Filter
        SearchFilterBar(
            searchQuery = state.searchQuery,
            filter = state.filter,
            onSearch = { viewModel.onIntent(TrackerIntent.Search(it)) },
            onFilter = { /* Filter dialog */ }
        )
        
        when (state.viewType) {
            TrackerViewType.KANBAN -> KanbanBoard(
                applicationsByStatus = state.applicationsByStatus,
                onMoveApplication = { appId, newStatus ->
                    viewModel.onIntent(TrackerIntent.MoveToStatus(appId, newStatus))
                },
                onApplicationClick = { /* Navigate to detail */ }
            )
            TrackerViewType.LIST -> ApplicationsList(
                applications = state.applications,
                onApplicationClick = { /* Navigate to detail */ }
            )
            TrackerViewType.CALENDAR -> CalendarView(
                applications = state.applications,
                onDateClick = { /* Show applications for date */ }
            )
        }
    }
}

@Composable
fun KanbanBoard(
    applicationsByStatus: Map<ApplicationStatus, List<JobApplication>>,
    onMoveApplication: (String, ApplicationStatus) -> Unit,
    onApplicationClick: (JobApplication) -> Unit
) {
    val visibleStatuses = listOf(
        ApplicationStatus.SAVED,
        ApplicationStatus.APPLIED,
        ApplicationStatus.INTERVIEW,
        ApplicationStatus.OFFER
    )
    
    LazyRow(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(visibleStatuses) { status ->
            KanbanColumn(
                status = status,
                applications = applicationsByStatus[status] ?: emptyList(),
                onMoveApplication = onMoveApplication,
                onApplicationClick = onApplicationClick
            )
        }
    }
}

@Composable
fun KanbanColumn(
    status: ApplicationStatus,
    applications: List<JobApplication>,
    onMoveApplication: (String, ApplicationStatus) -> Unit,
    onApplicationClick: (JobApplication) -> Unit
) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(status.color))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = applications.size.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            // Applications
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(applications, key = { it.id }) { application ->
                    ApplicationCard(
                        application = application,
                        onClick = { onApplicationClick(application) }
                    )
                }
            }
        }
    }
}
```

---

### 2.2 Chrome Extension

#### 2.2.1 Extension Architecture
**Effort:** 10 days  
**Owner:** Web Team

**File Structure:**
```
chrome-extension/
├── manifest.json
├── background.js
├── content.js
├── popup/
│   ├── popup.html
│   ├── popup.css
│   └── popup.js
├── options/
│   ├── options.html
│   └── options.js
├── icons/
│   ├── icon16.png
│   ├── icon48.png
│   └── icon128.png
└── lib/
    └── api.js
```

**Manifest V3:**
```json
// chrome-extension/manifest.json
{
  "manifest_version": 3,
  "name": "VwaTek Apply - Job Saver",
  "version": "1.0.0",
  "description": "Save jobs from any job board to VwaTek Apply with one click",
  "permissions": [
    "storage",
    "activeTab",
    "contextMenus"
  ],
  "host_permissions": [
    "https://www.indeed.com/*",
    "https://www.indeed.ca/*",
    "https://www.linkedin.com/jobs/*",
    "https://www.jobbank.gc.ca/*",
    "https://www.glassdoor.ca/*",
    "https://*.workday.com/*",
    "https://api.vwatek.com/*"
  ],
  "background": {
    "service_worker": "background.js"
  },
  "content_scripts": [
    {
      "matches": [
        "https://www.indeed.com/*",
        "https://www.indeed.ca/*"
      ],
      "js": ["content-scripts/indeed.js"],
      "css": ["content-scripts/styles.css"]
    },
    {
      "matches": ["https://www.linkedin.com/jobs/*"],
      "js": ["content-scripts/linkedin.js"],
      "css": ["content-scripts/styles.css"]
    },
    {
      "matches": ["https://www.jobbank.gc.ca/*"],
      "js": ["content-scripts/jobbank.js"],
      "css": ["content-scripts/styles.css"]
    }
  ],
  "action": {
    "default_popup": "popup/popup.html",
    "default_icon": {
      "16": "icons/icon16.png",
      "48": "icons/icon48.png",
      "128": "icons/icon128.png"
    }
  },
  "icons": {
    "16": "icons/icon16.png",
    "48": "icons/icon48.png",
    "128": "icons/icon128.png"
  }
}
```

**Indeed Content Script:**
```javascript
// chrome-extension/content-scripts/indeed.js
class IndeedJobExtractor {
  constructor() {
    this.jobData = null;
    this.init();
  }

  init() {
    // Add save button to job cards
    this.observeJobCards();
    
    // Extract current job if on job detail page
    if (this.isJobDetailPage()) {
      this.extractJobDetail();
    }
  }

  isJobDetailPage() {
    return window.location.pathname.includes('/viewjob') || 
           document.querySelector('[data-testid="jobsearch-ViewJobButtons"]');
  }

  extractJobDetail() {
    const jobData = {
      source: 'INDEED',
      externalId: this.getJobId(),
      title: this.getJobTitle(),
      company: this.getCompanyName(),
      location: this.getLocation(),
      salary: this.getSalary(),
      description: this.getDescription(),
      url: window.location.href,
      extractedAt: new Date().toISOString()
    };

    this.jobData = jobData;
    this.addSaveButton();
    return jobData;
  }

  getJobId() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('jk') || document.querySelector('[data-jk]')?.dataset.jk;
  }

  getJobTitle() {
    return document.querySelector('[data-testid="jobsearch-JobInfoHeader-title"]')?.textContent?.trim() ||
           document.querySelector('.jobsearch-JobInfoHeader-title')?.textContent?.trim();
  }

  getCompanyName() {
    return document.querySelector('[data-testid="jobsearch-CompanyInfoContainer"] a')?.textContent?.trim() ||
           document.querySelector('.jobsearch-CompanyInfoContainer a')?.textContent?.trim();
  }

  getLocation() {
    const locationEl = document.querySelector('[data-testid="jobsearch-JobInfoHeader-companyLocation"]') ||
                      document.querySelector('.jobsearch-JobInfoHeader-companyLocation');
    const text = locationEl?.textContent?.trim() || '';
    
    // Parse Canadian location
    const provinces = {
      'ON': 'Ontario', 'BC': 'British Columbia', 'AB': 'Alberta',
      'QC': 'Quebec', 'MB': 'Manitoba', 'SK': 'Saskatchewan',
      'NS': 'Nova Scotia', 'NB': 'New Brunswick', 'NL': 'Newfoundland',
      'PE': 'Prince Edward Island', 'NT': 'Northwest Territories',
      'NU': 'Nunavut', 'YT': 'Yukon'
    };

    let city = text;
    let province = null;
    let isRemote = text.toLowerCase().includes('remote');

    for (const [code, name] of Object.entries(provinces)) {
      if (text.includes(code) || text.includes(name)) {
        province = code;
        city = text.replace(code, '').replace(name, '').replace(',', '').trim();
        break;
      }
    }

    return { city, province, isRemote };
  }

  getSalary() {
    const salaryEl = document.querySelector('[data-testid="jobsearch-JobMetadataFooter-salary"]') ||
                    document.querySelector('.jobsearch-JobMetadataFooter-salary');
    const text = salaryEl?.textContent || '';
    
    // Parse salary: "$50,000 - $70,000 a year" or "$25 - $35 an hour"
    const match = text.match(/\$?([\d,]+)(?:\s*-\s*\$?([\d,]+))?\s*(an?\s*(?:hour|year))?/i);
    
    if (match) {
      const min = parseInt(match[1].replace(/,/g, ''));
      const max = match[2] ? parseInt(match[2].replace(/,/g, '')) : min;
      const period = match[3]?.toLowerCase().includes('hour') ? 'HOURLY' : 'ANNUAL';
      
      return { min, max, period, currency: 'CAD' };
    }
    
    return null;
  }

  getDescription() {
    return document.querySelector('#jobDescriptionText')?.innerHTML ||
           document.querySelector('.jobsearch-jobDescriptionText')?.innerHTML;
  }

  addSaveButton() {
    // Don't add if already exists
    if (document.querySelector('.vwatek-save-btn')) return;

    const buttonContainer = document.querySelector('[data-testid="jobsearch-ViewJobButtons"]') ||
                           document.querySelector('.jobsearch-ViewJobButtonsContainer');
    
    if (!buttonContainer) return;

    const saveBtn = document.createElement('button');
    saveBtn.className = 'vwatek-save-btn';
    saveBtn.innerHTML = `
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
        <polyline points="17 21 17 13 7 13 7 21"></polyline>
        <polyline points="7 3 7 8 15 8"></polyline>
      </svg>
      Save to VwaTek
    `;

    saveBtn.addEventListener('click', () => this.saveJob());
    buttonContainer.appendChild(saveBtn);
  }

  async saveJob() {
    const btn = document.querySelector('.vwatek-save-btn');
    btn.classList.add('loading');
    btn.textContent = 'Saving...';

    try {
      const response = await chrome.runtime.sendMessage({
        action: 'saveJob',
        job: this.jobData
      });

      if (response.success) {
        btn.classList.remove('loading');
        btn.classList.add('success');
        btn.textContent = 'Saved!';
      } else {
        throw new Error(response.error);
      }
    } catch (error) {
      btn.classList.remove('loading');
      btn.classList.add('error');
      btn.textContent = 'Error - Try Again';
      console.error('VwaTek: Failed to save job', error);
    }
  }

  observeJobCards() {
    const observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        mutation.addedNodes.forEach((node) => {
          if (node.nodeType === 1 && node.classList?.contains('job_seen_beacon')) {
            this.addQuickSaveButton(node);
          }
        });
      });
    });

    observer.observe(document.body, { childList: true, subtree: true });
    
    // Add to existing cards
    document.querySelectorAll('.job_seen_beacon').forEach(card => {
      this.addQuickSaveButton(card);
    });
  }

  addQuickSaveButton(card) {
    if (card.querySelector('.vwatek-quick-save')) return;

    const btn = document.createElement('button');
    btn.className = 'vwatek-quick-save';
    btn.title = 'Save to VwaTek';
    btn.innerHTML = '+';
    
    btn.addEventListener('click', async (e) => {
      e.preventDefault();
      e.stopPropagation();
      
      const jobId = card.dataset.jk;
      // Quick extract from card
      const title = card.querySelector('.jobTitle')?.textContent?.trim();
      const company = card.querySelector('.companyName')?.textContent?.trim();
      
      await chrome.runtime.sendMessage({
        action: 'quickSaveJob',
        job: {
          source: 'INDEED',
          externalId: jobId,
          title,
          company,
          url: `https://www.indeed.ca/viewjob?jk=${jobId}`
        }
      });
      
      btn.classList.add('saved');
      btn.innerHTML = '✓';
    });

    card.style.position = 'relative';
    card.appendChild(btn);
  }
}

// Initialize
new IndeedJobExtractor();
```

**Background Service Worker:**
```javascript
// chrome-extension/background.js
const API_BASE = 'https://api.vwatek.com/api/v1';

// Handle messages from content scripts
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.action === 'saveJob') {
    saveJobToVwaTek(message.job)
      .then(result => sendResponse({ success: true, data: result }))
      .catch(error => sendResponse({ success: false, error: error.message }));
    return true; // Keep channel open for async response
  }
  
  if (message.action === 'quickSaveJob') {
    quickSaveJob(message.job)
      .then(result => sendResponse({ success: true, data: result }))
      .catch(error => sendResponse({ success: false, error: error.message }));
    return true;
  }
});

async function getAuthToken() {
  const result = await chrome.storage.local.get(['authToken']);
  if (!result.authToken) {
    throw new Error('Not logged in. Please open the extension popup to sign in.');
  }
  return result.authToken;
}

async function saveJobToVwaTek(jobData) {
  const token = await getAuthToken();
  
  const response = await fetch(`${API_BASE}/jobs`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      jobTitle: jobData.title,
      companyName: jobData.company,
      jobUrl: jobData.url,
      jobBoardSource: jobData.source,
      externalJobId: jobData.externalId,
      city: jobData.location?.city,
      province: jobData.location?.province,
      isRemote: jobData.location?.isRemote || false,
      salaryMin: jobData.salary?.min,
      salaryMax: jobData.salary?.max,
      salaryPeriod: jobData.salary?.period,
      salaryCurrency: jobData.salary?.currency || 'CAD',
      description: jobData.description,
      status: 'SAVED'
    })
  });

  if (!response.ok) {
    throw new Error(`API error: ${response.status}`);
  }

  return response.json();
}

async function quickSaveJob(jobData) {
  // Simplified save for quick-save from job cards
  const token = await getAuthToken();
  
  const response = await fetch(`${API_BASE}/jobs/quick`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      jobTitle: jobData.title,
      companyName: jobData.company,
      jobUrl: jobData.url,
      jobBoardSource: jobData.source,
      externalJobId: jobData.externalId,
      status: 'SAVED'
    })
  });

  if (!response.ok) {
    throw new Error(`API error: ${response.status}`);
  }

  // Show badge notification
  chrome.action.setBadgeText({ text: '✓', tabId: sender?.tab?.id });
  setTimeout(() => chrome.action.setBadgeText({ text: '' }), 2000);

  return response.json();
}

// Context menu for right-click save
chrome.runtime.onInstalled.addListener(() => {
  chrome.contextMenus.create({
    id: 'saveToVwaTek',
    title: 'Save to VwaTek Apply',
    contexts: ['link', 'selection']
  });
});

chrome.contextMenus.onClicked.addListener((info, tab) => {
  if (info.menuItemId === 'saveToVwaTek') {
    // Extract job info from link or selection
    chrome.tabs.sendMessage(tab.id, {
      action: 'extractAndSave',
      url: info.linkUrl,
      selection: info.selectionText
    });
  }
});
```

---

### 2.3 Push Notifications & Reminders

#### 2.3.1 Notification Service
**Effort:** 5 days  
**Owner:** Platform Team

```kotlin
// shared/src/commonMain/kotlin/com/vwatek/apply/notifications/NotificationManager.kt
interface NotificationManager {
    suspend fun scheduleReminder(reminder: ApplicationReminder)
    suspend fun cancelReminder(reminderId: String)
    suspend fun requestPermission(): Boolean
    fun observePermissionStatus(): Flow<NotificationPermission>
}

data class NotificationPayload(
    val id: String,
    val title: String,
    val body: String,
    val deepLink: String?,
    val imageUrl: String?,
    val data: Map<String, String> = emptyMap()
)

enum class NotificationPermission {
    GRANTED, DENIED, NOT_DETERMINED
}

// Android Implementation
actual class NotificationManagerImpl(
    private val context: Context,
    private val workManager: WorkManager
) : NotificationManager {
    
    override suspend fun scheduleReminder(reminder: ApplicationReminder) {
        val data = workDataOf(
            "reminder_id" to reminder.id,
            "application_id" to reminder.applicationId,
            "title" to reminder.title,
            "message" to reminder.message
        )
        
        val delay = reminder.reminderAt.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds()
        
        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("reminder_${reminder.id}")
            .build()
        
        workManager.enqueue(workRequest)
    }
    
    override suspend fun cancelReminder(reminderId: String) {
        workManager.cancelAllWorkByTag("reminder_$reminderId")
    }
}

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val reminderId = inputData.getString("reminder_id") ?: return Result.failure()
        val applicationId = inputData.getString("application_id") ?: return Result.failure()
        val title = inputData.getString("title") ?: "VwaTek Reminder"
        val message = inputData.getString("message") ?: ""
        
        val notification = NotificationCompat.Builder(applicationContext, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createDeepLinkIntent(applicationId))
            .build()
        
        NotificationManagerCompat.from(applicationContext)
            .notify(reminderId.hashCode(), notification)
        
        return Result.success()
    }
}
```

---

### Phase 2 Deliverables Checklist

| Deliverable | Owner | Status |
|-------------|-------|--------|
| Job Application Data Model | Backend | ⬜ |
| Job Tracker API Endpoints | Backend | ⬜ |
| Kanban Board UI (Android) | Mobile | ⬜ |
| Kanban Board UI (iOS) | Mobile | ⬜ |
| Kanban Board UI (Web) | Web | ⬜ |
| List View | UI | ⬜ |
| Calendar View | UI | ⬜ |
| Chrome Extension - Indeed | Web | ⬜ |
| Chrome Extension - LinkedIn | Web | ⬜ |
| Chrome Extension - Job Bank | Web | ⬜ |
| Push Notification System | Platform | ⬜ |
| Reminder Scheduling | Platform | ⬜ |
| Application Statistics Dashboard | UI | ⬜ |

---

## Phase 3: Canadian Market Differentiation

**Duration:** 8-10 weeks  
**Priority:** HIGH  
**Theme:** "Built for Canada"

### 3.1 NOC Code Integration

#### 3.1.1 NOC Database
**Effort:** 5 days  
**Owner:** Backend Team

```kotlin
// backend/src/main/kotlin/com/vwatek/apply/db/tables/NOCTables.kt

// NOC 2021 Classification (TEER System)
object NOCCodesTable : Table("noc_codes") {
    val code = varchar("code", 10) // e.g., "21231"
    val titleEn = varchar("title_en", 255)
    val titleFr = varchar("title_fr", 255)
    val teerLevel = integer("teer_level") // 0-5
    val category = varchar("category", 10) // Broad category
    val majorGroup = varchar("major_group", 10)
    val subMajorGroup = varchar("sub_major_group", 10)
    val minorGroup = varchar("minor_group", 10)
    val unitGroup = varchar("unit_group", 10)
    val descriptionEn = text("description_en")
    val descriptionFr = text("description_fr")
    val leadStatementEn = text("lead_statement_en")
    val leadStatementFr = text("lead_statement_fr")
    
    override val primaryKey = PrimaryKey(code)
}

object NOCMainDutiesTable : Table("noc_main_duties") {
    val id = varchar("id", 36)
    val nocCode = varchar("noc_code", 10).references(NOCCodesTable.code)
    val dutyEn = text("duty_en")
    val dutyFr = text("duty_fr")
    val orderIndex = integer("order_index")
    
    override val primaryKey = PrimaryKey(id)
}

object NOCEmploymentRequirementsTable : Table("noc_employment_requirements") {
    val id = varchar("id", 36)
    val nocCode = varchar("noc_code", 10).references(NOCCodesTable.code)
    val requirementEn = text("requirement_en")
    val requirementFr = text("requirement_fr")
    val orderIndex = integer("order_index")
    
    override val primaryKey = PrimaryKey(id)
}

object NOCAdditionalInfoTable : Table("noc_additional_info") {
    val nocCode = varchar("noc_code", 10).references(NOCCodesTable.code)
    val exampleTitlesEn = text("example_titles_en") // JSON array
    val exampleTitlesFr = text("example_titles_fr")
    val classifiedElsewhereEn = text("classified_elsewhere_en") // JSON array
    val classifiedElsewhereFr = text("classified_elsewhere_fr")
    val exclusionsEn = text("exclusions_en")
    val exclusionsFr = text("exclusions_fr")
    
    override val primaryKey = PrimaryKey(nocCode)
}

// TEER Level descriptions
enum class TEERLevel(val level: Int, val title: String, val educationRequirement: String) {
    TEER_0(0, "Management", "University degree or significant experience"),
    TEER_1(1, "Professional", "University degree"),  
    TEER_2(2, "Technical", "College diploma, apprenticeship, or 2+ years training"),
    TEER_3(3, "Skilled", "College diploma, apprenticeship, or less than 2 years training"),
    TEER_4(4, "Intermediate", "High school diploma and on-the-job training"),
    TEER_5(5, "Labourer", "Short demonstrations or on-the-job training")
}
```

#### 3.1.2 NOC Matching AI
**Effort:** 8 days  
**Owner:** AI Team

```kotlin
// shared/src/commonMain/kotlin/com/vwatek/apply/domain/usecase/NOCMatchingUseCase.kt
class NOCMatchingUseCase(
    private val aiService: AiService,
    private val nocRepository: NOCRepository
) {
    suspend fun detectNOCFromJobDescription(jobDescription: String): NOCMatchResult {
        // First, use AI to extract key information
        val extractedInfo = aiService.extractJobInfo(jobDescription)
        
        // Then match against NOC database
        val potentialMatches = nocRepository.searchByKeywords(
            keywords = extractedInfo.skills + extractedInfo.duties,
            limit = 10
        )
        
        // Use AI to score and rank matches
        val scoredMatches = potentialMatches.map { noc ->
            val score = aiService.scoreNOCMatch(
                jobDescription = jobDescription,
                nocCode = noc.code,
                nocTitle = noc.titleEn,
                nocDuties = noc.mainDuties,
                nocRequirements = noc.employmentRequirements
            )
            NOCMatch(noc = noc, confidenceScore = score)
        }.sortedByDescending { it.confidenceScore }
        
        return NOCMatchResult(
            topMatch = scoredMatches.firstOrNull(),
            alternatives = scoredMatches.drop(1).take(3),
            extractedJobInfo = extractedInfo
        )
    }
    
    suspend fun analyzeResumeNOCFit(
        resume: Resume,
        targetNOCCode: String
    ): NOCFitAnalysis {
        val nocDetails = nocRepository.getByCode(targetNOCCode)
            ?: throw IllegalArgumentException("Invalid NOC code")
        
        val prompt = """
            Analyze how well this resume fits the NOC ${nocDetails.code} (${nocDetails.titleEn}) requirements.
            
            NOC TEER Level: ${nocDetails.teerLevel} (${TEERLevel.entries.find { it.level == nocDetails.teerLevel }?.educationRequirement})
            
            NOC Main Duties:
            ${nocDetails.mainDuties.joinToString("\n") { "- ${it.dutyEn}" }}
            
            NOC Employment Requirements:
            ${nocDetails.employmentRequirements.joinToString("\n") { "- ${it.requirementEn}" }}
            
            RESUME:
            ${resume.content}
            
            Provide JSON response:
            {
                "overallFitScore": <0-100>,
                "teerLevelFit": "EXCEEDS" | "MEETS" | "BELOW",
                "dutiesMatch": {
                    "matched": ["duty1", "duty2"],
                    "partialMatch": ["duty3"],
                    "missing": ["duty4"]
                },
                "requirementsMatch": {
                    "met": ["req1"],
                    "partiallyMet": ["req2"],
                    "notMet": ["req3"]
                },
                "immigrationReadiness": {
                    "expressEntryPoints": <estimated CRS points for work experience>,
                    "provincialNomineeEligibility": ["ON", "BC", "AB"],
                    "recommendations": ["recommendation1", "recommendation2"]
                },
                "resumeImprovements": [
                    {
                        "section": "Experience",
                        "suggestion": "Add specific duty mention",
                        "example": "Developed software applications using..."
                    }
                ]
            }
        """.trimIndent()
        
        return aiService.generateStructuredResponse<NOCFitAnalysis>(prompt)
    }
}

data class NOCMatchResult(
    val topMatch: NOCMatch?,
    val alternatives: List<NOCMatch>,
    val extractedJobInfo: ExtractedJobInfo
)

data class NOCMatch(
    val noc: NOCCode,
    val confidenceScore: Float // 0.0 - 1.0
)

data class NOCFitAnalysis(
    val overallFitScore: Int,
    val teerLevelFit: TeerFit,
    val dutiesMatch: DutiesMatchResult,
    val requirementsMatch: RequirementsMatchResult,
    val immigrationReadiness: ImmigrationReadiness,
    val resumeImprovements: List<ImprovementSuggestion>
)

enum class TeerFit { EXCEEDS, MEETS, BELOW }
```

---

### 3.2 Job Bank Integration

#### 3.2.1 Job Bank API Client
**Effort:** 5 days  
**Owner:** Backend Team

```kotlin
// backend/src/main/kotlin/com/vwatek/apply/integrations/jobbank/JobBankApiClient.kt
class JobBankApiClient(
    private val httpClient: HttpClient
) {
    companion object {
        private const val BASE_URL = "https://jobbank.gc.ca/api"
        private const val USER_AGENT = "VwaTek-Apply/1.0"
    }
    
    suspend fun searchJobs(
        query: String,
        location: String? = null,
        nocCode: String? = null,
        page: Int = 1,
        perPage: Int = 25
    ): JobBankSearchResult {
        val response = httpClient.get("$BASE_URL/jobs") {
            header("User-Agent", USER_AGENT)
            parameter("q", query)
            location?.let { parameter("location", it) }
            nocCode?.let { parameter("noc", it) }
            parameter("page", page)
            parameter("per_page", perPage)
        }
        
        return response.body<JobBankSearchResult>()
    }
    
    suspend fun getJobDetails(jobId: String): JobBankJobDetail {
        val response = httpClient.get("$BASE_URL/jobs/$jobId") {
            header("User-Agent", USER_AGENT)
        }
        
        return response.body<JobBankJobDetail>()
    }
    
    suspend fun getJobsByNOC(nocCode: String, page: Int = 1): JobBankSearchResult {
        return searchJobs(query = "", nocCode = nocCode, page = page)
    }
}

@Serializable
data class JobBankSearchResult(
    val jobs: List<JobBankJob>,
    val totalCount: Int,
    val page: Int,
    val perPage: Int
)

@Serializable
data class JobBankJob(
    val jobId: String,
    val title: String,
    val employer: String,
    val location: JobBankLocation,
    val salary: JobBankSalary?,
    val nocCode: String?,
    val postingDate: String,
    val expiryDate: String?,
    val jobUrl: String
)

@Serializable
data class JobBankLocation(
    val city: String,
    val province: String,
    val postalCode: String?,
    val isRemote: Boolean
)

@Serializable
data class JobBankSalary(
    val min: Double?,
    val max: Double?,
    val type: String, // "HOURLY", "ANNUAL"
    val currency: String // Always "CAD"
)
```

---

### 3.3 French Language Support

#### 3.3.1 Internationalization Setup
**Effort:** 10 days  
**Owner:** Platform Team

```kotlin
// shared/src/commonMain/kotlin/com/vwatek/apply/i18n/Strings.kt
interface Strings {
    // Common
    val appName: String
    val save: String
    val cancel: String
    val delete: String
    val edit: String
    val loading: String
    val error: String
    val retry: String
    
    // Navigation
    val navResume: String
    val navOptimizer: String
    val navCoverLetter: String
    val navInterview: String
    val navTracker: String
    val navSettings: String
    
    // Resume
    val resumeTitle: String
    val resumeCreate: String
    val resumeEdit: String
    val resumeAnalyze: String
    val resumeMatchScore: String
    val resumeKeywordsFound: String
    val resumeKeywordsMissing: String
    
    // Job Tracker
    val trackerTitle: String
    val trackerSaved: String
    val trackerApplied: String
    val trackerInterview: String
    val trackerOffer: String
    val trackerAddJob: String
    val trackerNoJobs: String
    
    // Canadian Specific
    val nocCode: String
    val nocMatchScore: String
    val workPermitRequired: String
    val lmiaRequired: String
    val province: String
    
    // Auth
    val signIn: String
    val signUp: String
    val signOut: String
    val forgotPassword: String
}

object EnglishStrings : Strings {
    override val appName = "VwaTek Apply"
    override val save = "Save"
    override val cancel = "Cancel"
    override val delete = "Delete"
    override val edit = "Edit"
    override val loading = "Loading..."
    override val error = "An error occurred"
    override val retry = "Retry"
    
    override val navResume = "Resume"
    override val navOptimizer = "Optimizer"
    override val navCoverLetter = "Cover Letter"
    override val navInterview = "Interview"
    override val navTracker = "Tracker"
    override val navSettings = "Settings"
    
    override val resumeTitle = "My Resumes"
    override val resumeCreate = "Create Resume"
    override val resumeEdit = "Edit Resume"
    override val resumeAnalyze = "Analyze Resume"
    override val resumeMatchScore = "Match Score"
    override val resumeKeywordsFound = "Keywords Found"
    override val resumeKeywordsMissing = "Missing Keywords"
    
    override val trackerTitle = "Job Tracker"
    override val trackerSaved = "Saved"
    override val trackerApplied = "Applied"
    override val trackerInterview = "Interview"
    override val trackerOffer = "Offer"
    override val trackerAddJob = "Add Job"
    override val trackerNoJobs = "No jobs tracked yet"
    
    override val nocCode = "NOC Code"
    override val nocMatchScore = "NOC Match Score"
    override val workPermitRequired = "Work Permit Required"
    override val lmiaRequired = "LMIA Required"
    override val province = "Province"
    
    override val signIn = "Sign In"
    override val signUp = "Sign Up"
    override val signOut = "Sign Out"
    override val forgotPassword = "Forgot Password?"
}

object FrenchStrings : Strings {
    override val appName = "VwaTek Apply"
    override val save = "Enregistrer"
    override val cancel = "Annuler"
    override val delete = "Supprimer"
    override val edit = "Modifier"
    override val loading = "Chargement..."
    override val error = "Une erreur s'est produite"
    override val retry = "Réessayer"
    
    override val navResume = "CV"
    override val navOptimizer = "Optimiseur"
    override val navCoverLetter = "Lettre de motivation"
    override val navInterview = "Entrevue"
    override val navTracker = "Suivi"
    override val navSettings = "Paramètres"
    
    override val resumeTitle = "Mes CV"
    override val resumeCreate = "Créer un CV"
    override val resumeEdit = "Modifier le CV"
    override val resumeAnalyze = "Analyser le CV"
    override val resumeMatchScore = "Score de correspondance"
    override val resumeKeywordsFound = "Mots-clés trouvés"
    override val resumeKeywordsMissing = "Mots-clés manquants"
    
    override val trackerTitle = "Suivi des emplois"
    override val trackerSaved = "Enregistré"
    override val trackerApplied = "Postulé"
    override val trackerInterview = "Entrevue"
    override val trackerOffer = "Offre"
    override val trackerAddJob = "Ajouter un emploi"
    override val trackerNoJobs = "Aucun emploi suivi"
    
    override val nocCode = "Code CNP"
    override val nocMatchScore = "Score CNP"
    override val workPermitRequired = "Permis de travail requis"
    override val lmiaRequired = "EIMT requise"
    override val province = "Province"
    
    override val signIn = "Connexion"
    override val signUp = "Inscription"
    override val signOut = "Déconnexion"
    override val forgotPassword = "Mot de passe oublié?"
}

// Locale Manager
class LocaleManager {
    private val _currentLocale = MutableStateFlow(Locale.ENGLISH)
    val currentLocale: StateFlow<Locale> = _currentLocale.asStateFlow()
    
    val strings: Strings
        get() = when (_currentLocale.value) {
            Locale.FRENCH -> FrenchStrings
            else -> EnglishStrings
        }
    
    fun setLocale(locale: Locale) {
        _currentLocale.value = locale
    }
}

enum class Locale(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    FRENCH("fr", "Français")
}
```

---

### Phase 3 Deliverables Checklist

| Deliverable | Owner | Status |
|-------------|-------|--------|
| NOC Database Import | Backend | ⬜ |
| NOC Search API | Backend | ⬜ |
| NOC Matching AI | AI Team | ⬜ |
| NOC Resume Fit Analysis | AI Team | ⬜ |
| Job Bank API Integration | Backend | ⬜ |
| Job Bank Search UI | UI | ⬜ |
| Internationalization Framework | Platform | ⬜ |
| English Strings Complete | Platform | ⬜ |
| French Strings Complete | Platform | ⬜ |
| French AI Prompts | AI Team | ⬜ |
| Language Switcher UI | UI | ⬜ |
| Provincial Resume Templates | UI | ⬜ |
| Work Authorization Indicators | UI | ⬜ |

---

## Phase 4: Premium & Monetization

**Duration:** 6-8 weeks  
**Priority:** MEDIUM  
**Theme:** "Sustainable business"

### 4.1 Subscription System

#### 4.1.1 Subscription Tiers
```kotlin
// shared/src/commonMain/kotlin/com/vwatek/apply/domain/model/Subscription.kt
enum class SubscriptionTier(
    val id: String,
    val displayName: String,
    val monthlyPriceCAD: Int,
    val yearlyPriceCAD: Int
) {
    FREE("free", "Free", 0, 0),
    PRO("pro", "Pro", 1499, 14999), // $14.99/mo or $149.99/yr
    PREMIUM("premium", "Premium", 2999, 29999) // $29.99/mo or $299.99/yr
}

data class FeatureLimits(
    val maxResumes: Int,
    val maxCoverLetters: Int,
    val aiAnalysesPerMonth: Int,
    val jobTrackerEnabled: Boolean,
    val cloudSyncEnabled: Boolean,
    val nocMatchingEnabled: Boolean,
    val salaryDataEnabled: Boolean,
    val linkedInOptimizerEnabled: Boolean,
    val prioritySupport: Boolean,
    val exportFormats: List<ExportFormat>
)

val TIER_LIMITS = mapOf(
    SubscriptionTier.FREE to FeatureLimits(
        maxResumes = 2,
        maxCoverLetters = 5,
        aiAnalysesPerMonth = 5,
        jobTrackerEnabled = true, // Limited to 10 jobs
        cloudSyncEnabled = false,
        nocMatchingEnabled = false,
        salaryDataEnabled = false,
        linkedInOptimizerEnabled = false,
        prioritySupport = false,
        exportFormats = listOf(ExportFormat.PDF)
    ),
    SubscriptionTier.PRO to FeatureLimits(
        maxResumes = -1, // Unlimited
        maxCoverLetters = -1,
        aiAnalysesPerMonth = 50,
        jobTrackerEnabled = true, // Unlimited
        cloudSyncEnabled = true,
        nocMatchingEnabled = true,
        salaryDataEnabled = false,
        linkedInOptimizerEnabled = false,
        prioritySupport = false,
        exportFormats = listOf(ExportFormat.PDF, ExportFormat.DOCX)
    ),
    SubscriptionTier.PREMIUM to FeatureLimits(
        maxResumes = -1,
        maxCoverLetters = -1,
        aiAnalysesPerMonth = -1, // Unlimited
        jobTrackerEnabled = true,
        cloudSyncEnabled = true,
        nocMatchingEnabled = true,
        salaryDataEnabled = true,
        linkedInOptimizerEnabled = true,
        prioritySupport = true,
        exportFormats = listOf(ExportFormat.PDF, ExportFormat.DOCX, ExportFormat.TXT)
    )
)
```

#### 4.1.2 Payment Integration (Stripe)
**Effort:** 10 days  
**Owner:** Backend Team

```kotlin
// backend/src/main/kotlin/com/vwatek/apply/payments/StripeService.kt
class StripeService(
    private val stripeApiKey: String
) {
    private val stripe = Stripe(stripeApiKey)
    
    suspend fun createCustomer(userId: String, email: String): String {
        val params = CustomerCreateParams.builder()
            .setEmail(email)
            .setMetadata(mapOf("userId" to userId))
            .build()
        
        val customer = Customer.create(params)
        return customer.id
    }
    
    suspend fun createSubscription(
        customerId: String,
        priceId: String // Stripe Price ID
    ): Subscription {
        val params = SubscriptionCreateParams.builder()
            .setCustomer(customerId)
            .addItem(
                SubscriptionCreateParams.Item.builder()
                    .setPrice(priceId)
                    .build()
            )
            .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
            .setPaymentSettings(
                SubscriptionCreateParams.PaymentSettings.builder()
                    .setSaveDefaultPaymentMethod(
                        SubscriptionCreateParams.PaymentSettings.SaveDefaultPaymentMethod.ON_SUBSCRIPTION
                    )
                    .build()
            )
            .addExpand("latest_invoice.payment_intent")
            .build()
        
        return Subscription.create(params)
    }
    
    suspend fun cancelSubscription(subscriptionId: String) {
        val subscription = Subscription.retrieve(subscriptionId)
        subscription.cancel()
    }
    
    suspend fun createPortalSession(customerId: String, returnUrl: String): String {
        val params = com.stripe.param.billingportal.SessionCreateParams.builder()
            .setCustomer(customerId)
            .setReturnUrl(returnUrl)
            .build()
        
        val session = com.stripe.model.billingportal.Session.create(params)
        return session.url
    }
}

// Webhook Handler
fun Route.stripeWebhook(stripeService: StripeService, subscriptionService: SubscriptionService) {
    post("/webhooks/stripe") {
        val payload = call.receiveText()
        val sigHeader = call.request.header("Stripe-Signature")
        
        val event = try {
            Webhook.constructEvent(payload, sigHeader, WEBHOOK_SECRET)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }
        
        when (event.type) {
            "customer.subscription.created",
            "customer.subscription.updated" -> {
                val subscription = event.dataObjectDeserializer.`object`.get() as Subscription
                subscriptionService.updateSubscription(subscription)
            }
            "customer.subscription.deleted" -> {
                val subscription = event.dataObjectDeserializer.`object`.get() as Subscription
                subscriptionService.cancelSubscription(subscription.id)
            }
            "invoice.payment_failed" -> {
                val invoice = event.dataObjectDeserializer.`object`.get() as Invoice
                subscriptionService.handlePaymentFailed(invoice.subscription)
            }
        }
        
        call.respond(HttpStatusCode.OK)
    }
}
```

---

### 4.2 Salary Intelligence

#### 4.2.1 Salary Data Model
**Effort:** 5 days  
**Owner:** Data Team

```kotlin
// backend/src/main/kotlin/com/vwatek/apply/db/tables/SalaryDataTables.kt

object SalaryDataTable : Table("salary_data") {
    val id = varchar("id", 36)
    val nocCode = varchar("noc_code", 10).references(NOCCodesTable.code).nullable()
    val jobTitle = varchar("job_title", 255)
    val normalizedTitle = varchar("normalized_title", 255) // For matching
    val province = varchar("province", 2)
    val city = varchar("city", 100).nullable()
    
    // Salary Statistics
    val medianSalary = integer("median_salary")
    val salary25thPercentile = integer("salary_25th_percentile")
    val salary75thPercentile = integer("salary_75th_percentile")
    val salaryMin = integer("salary_min")
    val salaryMax = integer("salary_max")
    
    // Metadata
    val sampleSize = integer("sample_size")
    val dataSource = varchar("data_source", 50) // STATS_CAN, JOB_BANK, GLASSDOOR
    val dataYear = integer("data_year")
    val lastUpdated = timestamp("last_updated")
    
    override val primaryKey = PrimaryKey(id)
}

object SalaryComparisonHistoryTable : Table("salary_comparison_history") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val jobTitle = varchar("job_title", 255)
    val offeredSalary = integer("offered_salary")
    val benchmarkMedian = integer("benchmark_median")
    val percentileRank = integer("percentile_rank") // Where offered falls
    val province = varchar("province", 2)
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
}
```

#### 4.2.2 Salary Intelligence Use Case
```kotlin
// shared/src/commonMain/kotlin/com/vwatek/apply/domain/usecase/SalaryIntelligenceUseCase.kt
class SalaryIntelligenceUseCase(
    private val salaryRepository: SalaryRepository,
    private val aiService: AiService
) {
    suspend fun getSalaryInsights(
        jobTitle: String,
        province: CanadianProvince,
        city: String? = null,
        yearsExperience: Int? = null
    ): SalaryInsights {
        // Get base salary data
        val salaryData = salaryRepository.findByTitleAndLocation(jobTitle, province, city)
            ?: salaryRepository.findByNOCAndLocation(
                nocCode = detectNOC(jobTitle),
                province = province
            )
        
        // Adjust for experience if provided
        val adjustedSalary = if (yearsExperience != null && salaryData != null) {
            adjustForExperience(salaryData, yearsExperience)
        } else {
            salaryData
        }
        
        return SalaryInsights(
            median = adjustedSalary?.medianSalary ?: 0,
            range = SalaryRange(
                min = adjustedSalary?.salaryMin ?: 0,
                max = adjustedSalary?.salaryMax ?: 0,
                percentile25 = adjustedSalary?.salary25thPercentile ?: 0,
                percentile75 = adjustedSalary?.salary75thPercentile ?: 0
            ),
            comparisons = getRegionalComparisons(jobTitle, province),
            marketTrend = getMarketTrend(jobTitle, province),
            dataConfidence = calculateConfidence(adjustedSalary),
            negotiationTips = generateNegotiationTips(jobTitle, adjustedSalary)
        )
    }
    
    suspend fun evaluateOffer(
        jobTitle: String,
        offeredSalary: Int,
        province: CanadianProvince,
        city: String? = null,
        benefits: List<String>? = null
    ): OfferEvaluation {
        val insights = getSalaryInsights(jobTitle, province, city)
        
        val percentileRank = calculatePercentileRank(offeredSalary, insights.range)
        
        val evaluation = when {
            percentileRank >= 75 -> OfferRating.EXCELLENT
            percentileRank >= 50 -> OfferRating.COMPETITIVE
            percentileRank >= 25 -> OfferRating.BELOW_AVERAGE
            else -> OfferRating.LOW
        }
        
        // AI-generated negotiation strategy
        val negotiationStrategy = if (evaluation != OfferRating.EXCELLENT) {
            aiService.generateNegotiationStrategy(
                jobTitle = jobTitle,
                offeredSalary = offeredSalary,
                marketMedian = insights.median,
                percentileRank = percentileRank
            )
        } else null
        
        return OfferEvaluation(
            rating = evaluation,
            percentileRank = percentileRank,
            comparedToMedian = ((offeredSalary - insights.median).toFloat() / insights.median * 100).toInt(),
            marketInsights = insights,
            negotiationStrategy = negotiationStrategy,
            totalCompensationEstimate = estimateTotalCompensation(offeredSalary, benefits)
        )
    }
}

data class SalaryInsights(
    val median: Int,
    val range: SalaryRange,
    val comparisons: List<RegionalComparison>,
    val marketTrend: MarketTrend,
    val dataConfidence: DataConfidence,
    val negotiationTips: List<String>
)

enum class OfferRating {
    EXCELLENT, COMPETITIVE, BELOW_AVERAGE, LOW
}
```

---

### Phase 4 Deliverables Checklist

| Deliverable | Owner | Status |
|-------------|-------|--------|
| Subscription Tier Model | Backend | ⬜ |
| Feature Gating System | Platform | ⬜ |
| Stripe Integration | Backend | ⬜ |
| iOS In-App Purchase | Mobile | ⬜ |
| Android In-App Purchase | Mobile | ⬜ |
| Subscription UI | UI | ⬜ |
| Salary Data Import Pipeline | Data | ⬜ |
| Salary API Endpoints | Backend | ⬜ |
| Salary Insights UI | UI | ⬜ |
| Offer Evaluation Feature | AI | ⬜ |
| Negotiation Coach AI | AI | ⬜ |

---

## Phase 5: Scale & Enterprise

**Duration:** 8-12 weeks  
**Priority:** MEDIUM  
**Theme:** "Growth and B2B"

### 5.1 LinkedIn Profile Optimizer

#### 5.1.1 LinkedIn Analysis
```kotlin
// shared/src/commonMain/kotlin/com/vwatek/apply/domain/usecase/LinkedInOptimizerUseCase.kt
class LinkedInOptimizerUseCase(
    private val aiService: AiService
) {
    suspend fun analyzeProfile(profileData: LinkedInProfileData): LinkedInAnalysis {
        val prompt = """
            Analyze this LinkedIn profile for a ${profileData.targetRole} in ${profileData.industry}.
            
            Current Profile:
            - Headline: ${profileData.headline}
            - Summary: ${profileData.summary}
            - Experience: ${profileData.experiences.joinToString("\n")}
            - Skills: ${profileData.skills.joinToString(", ")}
            
            Provide LinkedIn optimization analysis as JSON:
            {
                "overallScore": <0-100>,
                "headline": {
                    "score": <0-100>,
                    "issues": ["issue1"],
                    "optimized": "Better headline suggestion",
                    "reasoning": "Why this is better"
                },
                "summary": {
                    "score": <0-100>,
                    "issues": ["issue1"],
                    "optimized": "Better summary",
                    "keywords": ["keyword1", "keyword2"]
                },
                "experience": {
                    "score": <0-100>,
                    "improvements": [
                        {
                            "position": "Job Title",
                            "current": "Current bullet",
                            "improved": "Improved bullet"
                        }
                    ]
                },
                "skills": {
                    "score": <0-100>,
                    "missing": ["skill1", "skill2"],
                    "reorder": ["skill1", "skill2", "skill3"]
                },
                "seoKeywords": ["keyword1", "keyword2"],
                "searchAppearanceEstimate": "5x more likely to appear in recruiter searches"
            }
        """.trimIndent()
        
        return aiService.generateStructuredResponse<LinkedInAnalysis>(prompt)
    }
}
```

### 5.2 Enterprise Features

#### 5.2.1 Team/Organization Support
```kotlin
// backend/src/main/kotlin/com/vwatek/apply/db/tables/EnterpriseTables.kt

object OrganizationsTable : Table("organizations") {
    val id = varchar("id", 36)
    val name = varchar("name", 255)
    val domain = varchar("domain", 255).nullable() // For SSO
    val subscriptionTier = varchar("subscription_tier", 20)
    val maxSeats = integer("max_seats")
    val settings = text("settings") // JSON
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
}

object OrganizationMembersTable : Table("organization_members") {
    val id = varchar("id", 36)
    val organizationId = varchar("organization_id", 36).references(OrganizationsTable.id)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val role = varchar("role", 20) // ADMIN, MANAGER, MEMBER
    val joinedAt = timestamp("joined_at")
    
    override val primaryKey = PrimaryKey(id)
}

object OrganizationTemplatesTable : Table("organization_templates") {
    val id = varchar("id", 36)
    val organizationId = varchar("organization_id", 36).references(OrganizationsTable.id)
    val templateType = varchar("template_type", 30) // RESUME, COVER_LETTER
    val name = varchar("name", 255)
    val content = text("content")
    val isDefault = bool("is_default").default(false)
    val createdBy = varchar("created_by", 36).references(UsersTable.id)
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
}
```

---

### Phase 5 Deliverables Checklist

| Deliverable | Owner | Status |
|-------------|-------|--------|
| LinkedIn Profile Import | Backend | ⬜ |
| LinkedIn Optimizer AI | AI Team | ⬜ |
| LinkedIn Optimizer UI | UI | ⬜ |
| Organization Data Model | Backend | ⬜ |
| Team Management API | Backend | ⬜ |
| SSO Integration (SAML) | Backend | ⬜ |
| Admin Dashboard | Web | ⬜ |
| Team Templates | Full Stack | ⬜ |
| Reporting & Analytics | Data | ⬜ |
| API Documentation | Backend | ⬜ |
| White-Label Support | Platform | ⬜ |

---

## Technical Specifications

### API Versioning Strategy
```
/api/v1/* - Current stable API
/api/v2/* - Next version (breaking changes)
```

### Database Migration Strategy
- Use Flyway or Liquibase for versioned migrations
- Zero-downtime deployments with backwards-compatible changes
- Feature flags for gradual rollouts

### Performance Targets
| Metric | Target |
|--------|--------|
| API Response Time (p95) | < 200ms |
| AI Generation Time | < 5s |
| App Cold Start | < 2s |
| Sync Latency | < 3s |
| Uptime | 99.9% |

### Security Requirements
- All data encrypted at rest (AES-256)
- All data encrypted in transit (TLS 1.3)
- API rate limiting (100 req/min free, 500 req/min paid)
- OWASP Top 10 compliance
- Regular penetration testing

---

## Success Metrics

### Phase 1 Success Criteria
- [ ] 99.9% crash-free sessions
- [ ] <5% sync conflict rate
- [ ] Zero PIPEDA compliance issues

### Phase 2 Success Criteria
- [ ] 50% of active users using job tracker
- [ ] 10,000+ Chrome extension installs
- [ ] 30% DAU/MAU ratio

### Phase 3 Success Criteria
- [ ] 20% of users are French-speaking
- [ ] 5,000+ Job Bank jobs saved monthly
- [ ] 80% NOC detection accuracy

### Phase 4 Success Criteria
- [ ] 5% free-to-paid conversion rate
- [ ] $50K MRR within 6 months
- [ ] <5% monthly churn rate

### Phase 5 Success Criteria
- [ ] 10 enterprise customers
- [ ] $100K ARR from enterprise
- [ ] 20% of revenue from B2B

---

## Appendix

### A. NOC Data Source
- Statistics Canada NOC 2021 data files
- Job Bank API for job-NOC mapping
- Immigration, Refugees and Citizenship Canada (IRCC) Express Entry data

### B. Salary Data Sources
- Statistics Canada Labour Force Survey
- Job Bank wage data
- Glassdoor API (partnership)
- User-submitted (anonymized)

### C. Third-Party Dependencies
| Service | Purpose | Cost Estimate |
|---------|---------|---------------|
| Google Cloud Platform | Infrastructure | $500-2,000/mo |
| Stripe | Payments | 2.9% + $0.30 |
| Firebase | Crash reporting, Auth | $25-100/mo |
| Sentry | Error tracking | $26-89/mo |
| Mixpanel | Analytics | $0-89/mo |

---

**Document Maintained By:** VwaTek Engineering Team  
**Last Updated:** February 11, 2026  
**Next Review:** March 11, 2026
