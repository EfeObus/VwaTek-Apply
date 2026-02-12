# Architecture Guide

## Overview

VwaTek Apply follows Clean Architecture principles combined with the MVI (Model-View-Intent) pattern, ensuring a scalable, testable, and maintainable codebase across the Kotlin Multiplatform project targeting iOS, Android, and Web.

## Architecture Diagram

```
+------------------------------------------------------------------------+
|                iOS / Android / Web Application                          |
|  +------------------------------------------------------------------+  |
|  |              SwiftUI / Compose UI / Compose for Web              |  |
|  |              (Views, Screens, Components)                        |  |
|  +------------------------------------------------------------------+  |
+------------------------------------------------------------------------+
                                    |
                                    v
+------------------------------------------------------------------------+
|                         Shared Module (KMP)                            |
|  +------------------------------------------------------------------+  |
|  |                    Presentation Layer                            |  |
|  |           (ViewModels, UI States, Intents)                       |  |
|  +------------------------------------------------------------------+  |
|                                    |                                    |
|                                    v                                    |
|  +------------------------------------------------------------------+  |
|  |                      Domain Layer                                |  |
|  |              (Use Cases, Business Logic)                         |  |
|  +------------------------------------------------------------------+  |
|                                    |                                    |
|                                    v                                    |
|  +------------------------------------------------------------------+  |
|  |                       Data Layer                                 |  |
|  |        (Repositories, Data Sources, Models)                      |  |
|  +------------------------------------------------------------------+  |
+------------------------------------------------------------------------+
                                    |
                    +---------------+---------------+
                    v               v               v
            +------------+   +------------+   +------------+
            | SQLDelight |   |   Ktor     |   |  Gemini    |
            | (Local DB) |   | (Network)  |   |   API      |
            +------------+   +------------+   +------------+
```

## Phase 1: Infrastructure Components (February 2026)

### Observability & Monitoring

```
+------------------------------------------------------------------+
|                    Observability Stack                            |
+------------------------------------------------------------------+
|                                                                   |
|  +---------------------------+  +---------------------------+     |
|  |  Firebase Crashlytics    |  |  Sentry (Web)             |     |
|  |  (Android & iOS)         |  |  Error tracking &         |     |
|  |  Crash reporting         |  |  performance monitoring   |     |
|  +---------------------------+  +---------------------------+     |
|                                                                   |
|  +---------------------------+  +---------------------------+     |
|  |  Shared Analytics        |  |  Backend APM              |     |
|  |  Cross-platform events   |  |  Micrometer + Prometheus  |     |
|  |  via AnalyticsTracker    |  |  /metrics endpoint        |     |
|  +---------------------------+  +---------------------------+     |
|                                                                   |
+------------------------------------------------------------------+
```

### Sync Architecture

```
+------------------------------------------------------------------+
|                      Sync Engine Flow                             |
+------------------------------------------------------------------+
|                                                                   |
|  Local Change  ──►  SyncEngine  ──►  SyncApiClient  ──►  Backend |
|      │                  │                                    │    |
|      │                  ▼                                    │    |
|      │         NetworkMonitor                                │    |
|      │         (Online/Offline)                              │    |
|      │                  │                                    │    |
|      ▼                  ▼                                    ▼    |
|  SQLDelight  ◄──  Conflict Resolution  ◄──  Server Changes       |
|                                                                   |
+------------------------------------------------------------------+
```

**Key Components:**
- `SyncEngine` - Platform-specific sync orchestration
- `SyncApiClient` - Backend communication for sync operations
- `NetworkMonitor` - Real-time connectivity status
- `SyncModels` - Change tracking data structures

### Privacy & Compliance (PIPEDA)

```
+------------------------------------------------------------------+
|                    Privacy Architecture                           |
+------------------------------------------------------------------+
|                                                                   |
|  ConsentManager  ──►  User Preferences  ──►  PrivacyApiClient    |
|        │                                           │              |
|        ▼                                           ▼              |
|  Platform Storage                           Backend Routes        |
|  (Encrypted)                                (/api/v1/privacy/*)   |
|        │                                           │              |
|        ▼                                           ▼              |
|  Consent Records                            Data Export/Delete    |
|  (Analytics, Data, Marketing)               PIPEDA Compliance     |
|                                                                   |
+------------------------------------------------------------------+
```

---

## Phase 2: Job Tracker Components (February 2026)

### Job Tracker Architecture

```
+------------------------------------------------------------------+
|                    Job Tracker Flow                               |
+------------------------------------------------------------------+
|                                                                   |
|   ┌───────────────┐      ┌───────────────┐      ┌─────────────┐  |
|   │  Platform UI  │      │   Shared KMP  │      │   Backend   │  |
|   └───────────────┘      └───────────────┘      └─────────────┘  |
|         │                       │                      │          |
|         ▼                       │                      │          |
|   TrackerScreen                 │                      │          |
|   (Android/iOS/Web)             │                      │          |
|         │                       │                      │          |
|         └──────────────►        │                      │          |
|                         TrackerViewModel               │          |
|                              │                         │          |
|                              ▼                         │          |
|                        TrackerUseCases ────────►JobTrackerApiClient
|                              │                         │          |
|                              │                         ▼          |
|                              │                 JobTrackerRoutes   |
|                              │                         │          |
|                              │                         ▼          |
|                              │                  JobTrackerTables  |
|                                                  (PostgreSQL)     |
+------------------------------------------------------------------+
```

### TrackerViewModel Architecture (MVI Pattern)

```
+------------------------------------------------------------------+
|              TrackerViewModel State Flow                          |
+------------------------------------------------------------------+
|                                                                   |
|  User Action (Intent)                                             |
|        │                                                          |
|        ▼                                                          |
|  ┌─────────────────────────────────────────────────────────────┐ |
|  │                    TrackerIntent                             │ |
|  │  LoadApplications | CreateApplication | UpdateStatus         │ |
|  │  AddNote | AddReminder | AddInterview | DeleteApplication    │ |
|  │  SetViewMode | SetFilterStatus | MoveToStatus               │ |
|  └─────────────────────────────────────────────────────────────┘ |
|        │                                                          |
|        ▼                                                          |
|  ┌─────────────────────────────────────────────────────────────┐ |
|  │                  TrackerViewModel                            │ |
|  │  - Handles intents via onIntent()                           │ |
|  │  - Delegates to UseCases                                    │ |
|  │  - Updates TrackerState                                      │ |
|  └─────────────────────────────────────────────────────────────┘ |
|        │                                                          |
|        ▼                                                          |
|  ┌─────────────────────────────────────────────────────────────┐ |
|  │                    TrackerState                              │ |
|  │  - applications: List<JobApplication>                        │ |
|  │  - kanbanColumns: Map<ApplicationStatus, KanbanColumn>       │ |
|  │  - selectedApplication: JobApplicationDetail?                │ |
|  │  - stats: TrackerStats?                                      │ |
|  │  - viewMode: TrackerViewMode (KANBAN/LIST/CALENDAR)          │ |
|  │  - isLoading, error, filters                                │ |
|  └─────────────────────────────────────────────────────────────┘ |
|        │                                                          |
|        ▼                                                          |
|  UI Collects StateFlow and renders                                |
|                                                                   |
+------------------------------------------------------------------+
```

### Use Cases Layer

```
+------------------------------------------------------------------+
|                TrackerUseCases (10 Implementations)               |
+------------------------------------------------------------------+
|                                                                   |
|  GetJobApplicationsUseCaseImpl ──►  apiClient.getApplications()  |
|  GetJobApplicationByIdUseCaseImpl ──►  apiClient.getApplication()|
|  CreateJobApplicationUseCaseImpl ──►  apiClient.createApplication|
|  UpdateJobApplicationUseCaseImpl ──►  apiClient.updateApplication|
|  UpdateApplicationStatusUseCaseImpl ──►  apiClient.updateStatus()|
|  DeleteJobApplicationUseCaseImpl ──►  apiClient.deleteApplication|
|  AddApplicationNoteUseCaseImpl ──►  apiClient.addNote()          |
|  AddApplicationReminderUseCaseImpl ──►  apiClient.addReminder()  |
|  AddApplicationInterviewUseCaseImpl ──►  apiClient.addInterview()|
|  GetTrackerStatsUseCaseImpl ──►  apiClient.getStats()            |
|                                                                   |
+------------------------------------------------------------------+
```

### Dependency Injection (Koin)

```kotlin
// Modules.kt - Tracker DI Configuration
val sharedModule = module {
    // API Client
    single { JobTrackerApiClient(get()) }
    
    // Use Cases (interface bindings for testability)
    factory<GetJobApplicationsUseCase> { GetJobApplicationsUseCaseImpl(get()) }
    factory<GetJobApplicationByIdUseCase> { GetJobApplicationByIdUseCaseImpl(get()) }
    factory<CreateJobApplicationUseCase> { CreateJobApplicationUseCaseImpl(get()) }
    factory<UpdateJobApplicationUseCase> { UpdateJobApplicationUseCaseImpl(get()) }
    factory<UpdateApplicationStatusUseCase> { UpdateApplicationStatusUseCaseImpl(get()) }
    factory<DeleteJobApplicationUseCase> { DeleteJobApplicationUseCaseImpl(get()) }
    factory<AddApplicationNoteUseCase> { AddApplicationNoteUseCaseImpl(get()) }
    factory<AddApplicationReminderUseCase> { AddApplicationReminderUseCaseImpl(get()) }
    factory<AddApplicationInterviewUseCase> { AddApplicationInterviewUseCaseImpl(get()) }
    factory<GetTrackerStatsUseCase> { GetTrackerStatsUseCaseImpl(get()) }
    
    // ViewModel
    factory { TrackerViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}
```

### Platform Implementations

| Platform | UI Component | ViewModel Access |
|----------|--------------|------------------|
| Android | `TrackerScreen.kt` (Compose) | `koinInject<TrackerViewModel>()` |
| iOS | `TrackerView.swift` (SwiftUI) | `TrackerViewModelWrapper` → `KoinHelper.getTrackerViewModel()` |
| Web | `TrackerScreen.kt` (Compose Web) | `GlobalContext.get().get<TrackerViewModel>()` |

---

## Layer Responsibilities

### 1. Presentation Layer

**Location:** `shared/src/commonMain/kotlin/presentation/`

The presentation layer handles UI state management and user interactions.

```kotlin
// Example ViewModel
class ResumeReviewViewModel(
    private val analyzeResumeUseCase: AnalyzeResumeUseCase,
    private val optimizeResumeUseCase: OptimizeResumeUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(ResumeReviewState())
    val state: StateFlow<ResumeReviewState> = _state.asStateFlow()
    
    fun onIntent(intent: ResumeReviewIntent) {
        when (intent) {
            is ResumeReviewIntent.AnalyzeResume -> analyzeResume(intent.resume, intent.jobDescription)
            is ResumeReviewIntent.OptimizeResume -> optimizeResume(intent.resume)
        }
    }
}
```

**Components:**
- **ViewModels**: Manage UI state and handle business logic delegation
- **UI States**: Immutable data classes representing screen states
- **Intents**: Sealed classes representing user actions

### 2. Domain Layer

**Location:** `shared/src/commonMain/kotlin/domain/`

The domain layer contains pure business logic with no dependencies on frameworks.

```kotlin
// Example Use Case
class AnalyzeResumeUseCase(
    private val resumeRepository: ResumeRepository,
    private val aiService: AiService
) {
    suspend operator fun invoke(
        resume: Resume,
        jobDescription: JobDescription
    ): Result<ResumeAnalysis> {
        return try {
            val analysis = aiService.analyzeResume(resume, jobDescription)
            resumeRepository.saveAnalysis(analysis)
            Result.success(analysis)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Components:**
- **Use Cases**: Single-responsibility business operations
- **Domain Models**: Core business entities
- **Repository Interfaces**: Abstractions for data access

### 3. Data Layer

**Location:** `shared/src/commonMain/kotlin/data/`

The data layer implements data access and external service integration.

```kotlin
// Example Repository Implementation
class ResumeRepositoryImpl(
    private val database: VwaTekDatabase,
    private val geminiApi: GeminiApiService
) : ResumeRepository {
    
    override suspend fun getResumes(): List<Resume> {
        return database.resumeQueries.selectAll().executeAsList().map { it.toDomain() }
    }
    
    override suspend fun saveResume(resume: Resume) {
        database.resumeQueries.insert(resume.toEntity())
    }
}
```

**Components:**
- **Repository Implementations**: Concrete data access logic
- **Data Sources**: Local (SQLDelight) and Remote (Ktor/Gemini)
- **Data Models**: DTOs and entity mappers

## Module Structure

### Shared Module

```
shared/
|-- src/
|   |-- commonMain/
|   |   |-- kotlin/
|   |   |   |-- com/vwatek/apply/
|   |   |   |   |-- analytics/          # Phase 1: Analytics framework
|   |   |   |   |   |-- Analytics.kt
|   |   |   |   |   +-- AnalyticsTracker.kt
|   |   |   |   |-- sync/               # Phase 1: Sync engine
|   |   |   |   |   |-- SyncModels.kt
|   |   |   |   |   +-- SyncEngine.kt
|   |   |   |   |-- privacy/            # Phase 1: PIPEDA consent
|   |   |   |   |   +-- ConsentManager.kt
|   |   |   |   |-- network/            # Phase 1: Connectivity
|   |   |   |   |   +-- NetworkMonitor.kt
|   |   |   |   |-- data/
|   |   |   |   |   |-- api/            # Phase 1: API clients
|   |   |   |   |   |   |-- ApiConfig.kt
|   |   |   |   |   |   |-- SyncApiClient.kt
|   |   |   |   |   |   +-- PrivacyApiClient.kt
|   |   |   |   |   |-- local/
|   |   |   |   |   |   |-- dao/
|   |   |   |   |   |   +-- entity/
|   |   |   |   |   |-- remote/
|   |   |   |   |   |   |-- api/
|   |   |   |   |   |   +-- dto/
|   |   |   |   |   +-- repository/
|   |   |   |   |-- domain/
|   |   |   |   |   |-- model/
|   |   |   |   |   |-- repository/
|   |   |   |   |   +-- usecase/
|   |   |   |   |       |-- resume/
|   |   |   |   |       |-- coverletter/
|   |   |   |   |       +-- interview/
|   |   |   |   |-- presentation/
|   |   |   |   |   |-- resume/
|   |   |   |   |   |-- coverletter/
|   |   |   |   |   +-- interview/
|   |   |   |   +-- di/
|   |   |   |       +-- Modules.kt
|   |   +-- sqldelight/
|   |       +-- com/vwatek/apply/
|   |           +-- VwaTekDatabase.sq
|   |-- androidMain/
|   |   +-- kotlin/
|   |       +-- com/vwatek/apply/
|   |           +-- Platform.android.kt
|   |-- iosMain/
|   |   +-- kotlin/
|   |       +-- com/vwatek/apply/
|   |           +-- Platform.ios.kt
|   +-- jsMain/
|       +-- kotlin/
|           +-- com/vwatek/apply/
|               +-- Platform.js.kt
```

## Dependency Injection

VwaTek Apply uses **Koin** for dependency injection across all layers.

```kotlin
// DI Modules
val dataModule = module {
    single { createDatabase(get()) }
    single { GeminiApiService(get()) }
    single<ResumeRepository> { ResumeRepositoryImpl(get(), get()) }
}

val domainModule = module {
    // Resume Analysis
    factory { AnalyzeResumeUseCase(get(), get()) }
    factory { OptimizeResumeUseCase(get()) }
    factory { PerformATSAnalysisUseCase(get()) }
    factory { RewriteSectionUseCase(get()) }
    
    // Version Control
    factory { GetResumeVersionsUseCase(get()) }
    factory { CreateResumeVersionUseCase(get()) }
    factory { RestoreResumeVersionUseCase(get()) }
    factory { DeleteResumeVersionUseCase(get()) }
    
    // Cover Letters & Interviews
    factory { GenerateCoverLetterUseCase(get()) }
    factory { MockInterviewUseCase(get()) }
}

val presentationModule = module {
    viewModel { ResumeReviewViewModel(get(), get()) }
    viewModel { ResumeViewModel(get(), get(), get(), get()) }
    viewModel { CoverLetterViewModel(get()) }
    viewModel { InterviewPrepViewModel(get()) }
}
```

## Data Flow

### MVI Pattern Flow

```
+------------------------------------------------------------------+
|                         User Action                               |
+------------------------------------------------------------------+
                                |
                                v
+------------------------------------------------------------------+
|                           Intent                                  |
|              (e.g., AnalyzeResumeIntent)                         |
+------------------------------------------------------------------+
                                |
                                v
+------------------------------------------------------------------+
|                         ViewModel                                 |
|                    (Process Intent)                               |
+------------------------------------------------------------------+
                                |
                                v
+------------------------------------------------------------------+
|                          Use Case                                 |
|                   (Business Logic)                                |
+------------------------------------------------------------------+
                                |
                                v
+------------------------------------------------------------------+
|                         Repository                                |
|                     (Data Access)                                 |
+------------------------------------------------------------------+
                                |
                                v
+------------------------------------------------------------------+
|                          State                                    |
|              (e.g., ResumeReviewState)                           |
+------------------------------------------------------------------+
                                |
                                v
+------------------------------------------------------------------+
|                            UI                                     |
|                    (Render State)                                 |
+------------------------------------------------------------------+
```

## AI Integration Architecture

### Gemini API Service

```kotlin
class GeminiApiService(
    private val httpClient: HttpClient,
    private val apiKey: String
) {
    suspend fun generateContent(
        prompt: String,
        streamResponse: Boolean = true
    ): Flow<String> = flow {
        // Stream response for real-time UI updates
        httpClient.preparePost("$BASE_URL/models/gemini-2.0-flash:streamGenerateContent") {
            parameter("key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(GenerateContentRequest(prompt))
        }.execute { response ->
            response.bodyAsChannel().toFlow().collect { chunk ->
                emit(chunk.decodeToString())
            }
        }
    }
    
    // ATS Analysis with structured output
    suspend fun performATSAnalysis(
        resumeContent: String,
        jobDescription: String
    ): ATSAnalysis
    
    // Section-specific rewriting
    suspend fun rewriteResumeSection(
        sectionType: SectionType,
        sectionContent: String,
        targetRole: String?,
        targetIndustry: String?,
        writingStyle: WritingStyle
    ): SectionRewriteResult
}
```

### AI Feature Pipeline

```
+---------------+     +---------------+     +---------------+
|   User Input  |---->| Prompt Engine |---->|  Gemini API   |
|   (Resume +   |     |  (Template +  |     |   (Stream)    |
|    Job Desc)  |     |   Context)    |     |               |
+---------------+     +---------------+     +---------------+
                                                    |
                                                    v
+---------------+     +---------------+     +---------------+
|   UI Update   |<----|   Parser      |<----|   Response    |
|  (Real-time)  |     |  (Structured  |     |   (Chunks)    |
|               |     |    Output)    |     |               |
+---------------+     +---------------+     +---------------+
```

## Database Schema

### SQLDelight Schema

```sql
-- Resume Table
CREATE TABLE Resume (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    content TEXT NOT NULL,
    industry TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Job Description Table
CREATE TABLE JobDescription (
    id TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    company TEXT NOT NULL,
    description TEXT NOT NULL,
    requirements TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

-- Analysis Table
CREATE TABLE Analysis (
    id TEXT PRIMARY KEY NOT NULL,
    resume_id TEXT NOT NULL,
    job_id TEXT NOT NULL,
    match_score REAL NOT NULL,
    missing_keywords TEXT NOT NULL,
    suggestions TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (resume_id) REFERENCES Resume(id),
    FOREIGN KEY (job_id) REFERENCES JobDescription(id)
);

-- Cover Letter Table
CREATE TABLE CoverLetter (
    id TEXT PRIMARY KEY NOT NULL,
    resume_id TEXT NOT NULL,
    job_id TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (resume_id) REFERENCES Resume(id),
    FOREIGN KEY (job_id) REFERENCES JobDescription(id)
);

-- Interview Session Table
CREATE TABLE InterviewSession (
    id TEXT PRIMARY KEY NOT NULL,
    job_id TEXT NOT NULL,
    transcript TEXT NOT NULL,
    feedback TEXT NOT NULL,
    score REAL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (job_id) REFERENCES JobDescription(id)
);

-- Resume Version Table (Version Control)
CREATE TABLE ResumeVersion (
    id TEXT PRIMARY KEY NOT NULL,
    resume_id TEXT NOT NULL,
    version_number INTEGER NOT NULL,
    content TEXT NOT NULL,
    change_description TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (resume_id) REFERENCES Resume(id)
);
```

## Domain Models

### Core Entities

```kotlin
// Resume with version tracking
@Serializable
data class Resume(
    val id: String,
    val name: String,
    val content: String,
    val industry: String? = null,
    val targetRole: String? = null,
    val currentVersionId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

// Version history entry
@Serializable
data class ResumeVersion(
    val id: String,
    val resumeId: String,
    val versionNumber: Int,
    val content: String,
    val changeDescription: String,
    val createdAt: Instant
) {
    val createdAtFormatted: String
        get() = createdAt.toString().take(19).replace("T", " ")
}

// ATS Analysis result
@Serializable
data class ATSAnalysis(
    val overallScore: Int,
    val formatScore: Int,
    val keywordScore: Int,
    val readabilityScore: Int,
    val sections: Map<String, SectionAnalysis>,
    val missingKeywords: MissingKeywords,
    val formatIssues: List<String>,
    val impactBullets: List<ImpactBullet>,
    val grammarIssues: List<GrammarIssue>
)

// Section rewrite result
@Serializable
data class SectionRewriteResult(
    val rewrittenContent: String,
    val changes: List<String>,
    val keywords: List<String>,
    val tips: List<String>
)
```

## Security Architecture

### Data Protection

```
+------------------------------------------------------------------+
|                      Security Layers                              |
+------------------------------------------------------------------+
|  1. SQLDelight Encryption (SQLCipher)                            |
|     - AES-256 encryption for local database                      |
|     - IndexedDB with encryption for Web                          |
|                                                                   |
|  2. Secure Key Storage                                           |
|     - iOS Keychain / Android Keystore for API keys               |
|     - Web Crypto API for browser storage                         |
|     - Encrypted SharedPreferences alternative                    |
|                                                                   |
|  3. Network Security                                             |
|     - TLS 1.3 for all API communications                         |
|     - Certificate pinning for Gemini API                         |
|                                                                   |
|  4. Data Minimization                                            |
|     - No server-side storage of user data                        |
|     - All processing done on-device or via API calls             |
+------------------------------------------------------------------+
```

## Testing Strategy

### Test Pyramid

```
                    +---------------+
                    |   E2E Tests   |
                    | (UI/XCTest/   |
                    |  Espresso)    |
                    +---------------+
                   /                 \
                  /                   \
        +-------------------------------+
        |      Integration Tests        |
        |   (Repository, API, DB)       |
        +-------------------------------+
       /                                 \
      /                                   \
+---------------------------------------------+
|              Unit Tests                      |
|   (Use Cases, ViewModels, Utilities)        |
+---------------------------------------------+
```

### Test Locations

- **Unit Tests:** `shared/src/commonTest/`
- **Integration Tests:** `shared/src/commonTest/integration/`
- **iOS UI Tests:** `iosApp/iosAppTests/`
- **Android UI Tests:** `androidApp/src/androidTest/`
- **Web Tests:** `webApp/src/jsTest/`

## Performance Considerations

1. **Lazy Loading**: Resume lists and analysis history load on-demand
2. **Caching**: AI responses cached locally to reduce API calls
3. **Streaming**: Real-time AI response display reduces perceived latency
4. **Background Processing**: Heavy computations offloaded to background threads
5. **Memory Management**: Proper lifecycle handling for Compose states

## Infrastructure Components (Phase 1)

### Analytics Tracking

```kotlin
// Cross-platform analytics interface
interface AnalyticsTracker {
    fun trackEvent(event: AnalyticsEvent)
    fun setUserProperty(key: String, value: String)
    fun setUserId(userId: String?)
}

// Platform implementations:
// - Android: Firebase Analytics
// - iOS: Firebase Analytics  
// - Web: Sentry/Custom analytics
```

### Network Monitoring

```kotlin
// Real-time connectivity status
interface NetworkMonitor {
    val networkState: StateFlow<NetworkState>
    fun startMonitoring()
    fun stopMonitoring()
}

data class NetworkState(
    val status: NetworkStatus,
    val type: NetworkType
)

enum class NetworkStatus { AVAILABLE, UNAVAILABLE, UNKNOWN }
enum class NetworkType { WIFI, CELLULAR, ETHERNET, UNKNOWN }
```

### Backend Monitoring

```kotlin
// Ktor server monitoring plugin
fun Application.configureMonitoring() {
    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) {
        registry = meterRegistry
    }
    routing {
        get("/metrics") {
            call.respondText(meterRegistry.scrape())
        }
    }
}
```

## Canadian Data Residency

**Region:** `northamerica-northeast1` (Montreal, Canada)

All infrastructure deployed in Canadian region for:
- PIPEDA compliance
- Quebec Law 25 compliance
- Data sovereignty requirements
- Lower latency for Canadian users

## Future Architecture Considerations

- **Feature Modules**: Split into feature-specific modules for better build times
- **Remote Config**: Dynamic feature flags for A/B testing
- ~~**Analytics Module**~~: ✅ Implemented in Phase 1
- ~~**Offline Support**~~: ✅ Sync engine implemented in Phase 1
- **Voice Interviews**: Real-time audio with Gemini Live (Phase 2)
- **Job Board Integration**: Canadian job sites API integration (Phase 3)
