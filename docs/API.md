# API Reference

## Overview

VwaTek Apply integrates with the **Google Gemini API** for all AI-powered features. This document covers the API integration patterns, request/response formats, and error handling strategies.

## Authentication

### API Key Configuration

Store your Gemini API key in `secrets.properties`:

```properties
GEMINI_API_KEY=your_actual_api_key_here
```

The key is loaded at build time and injected into the application:

```kotlin
// BuildKonfig access
val apiKey = BuildKonfig.GEMINI_API_KEY
```

### Secure Storage

For production, the API key is stored securely:

**iOS:**
```kotlin
// iOS-specific implementation
actual class SecureStorage {
    actual fun getApiKey(): String {
        return KeychainWrapper.standard.string(forKey: "gemini_api_key") ?: ""
    }
}
```

**Android:**
```kotlin
// Android-specific implementation
actual class SecureStorage(private val context: Context) {
    actual fun getApiKey(): String {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        return sharedPreferences.getString("gemini_api_key", "") ?: ""
    }
}
```

## Gemini API Integration

### Base Configuration

```kotlin
object GeminiConfig {
    const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
    const val MODEL = "gemini-2.0-flash"
    const val MAX_TOKENS = 8192
    const val TEMPERATURE = 0.7
}
```

### HTTP Client Setup

```kotlin
val httpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
    
    install(HttpTimeout) {
        requestTimeoutMillis = 60_000
        connectTimeoutMillis = 15_000
    }
    
    install(Logging) {
        level = LogLevel.BODY
    }
}
```

## API Endpoints

### 1. Resume Analysis

Analyze a resume against a job description to calculate match score and identify gaps.

**Endpoint:** `POST /models/gemini-2.0-flash:generateContent`

**Request:**

```kotlin
@Serializable
data class ResumeAnalysisRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String
)

@Serializable
data class GenerationConfig(
    val temperature: Double = 0.7,
    val maxOutputTokens: Int = 4096,
    val responseMimeType: String = "application/json"
)
```

**Example Request:**

```kotlin
suspend fun analyzeResume(resume: String, jobDescription: String): ResumeAnalysis {
    val prompt = """
        Analyze the following resume against the job description.
        
        RESUME:
        $resume
        
        JOB DESCRIPTION:
        $jobDescription
        
        Provide a JSON response with:
        {
            "matchScore": <0-100>,
            "matchedKeywords": ["keyword1", "keyword2"],
            "missingKeywords": ["keyword1", "keyword2"],
            "suggestions": ["suggestion1", "suggestion2"],
            "strengthAreas": ["area1", "area2"],
            "improvementAreas": ["area1", "area2"]
        }
    """.trimIndent()
    
    val request = ResumeAnalysisRequest(
        contents = listOf(Content(parts = listOf(Part(text = prompt)))),
        generationConfig = GenerationConfig(
            responseMimeType = "application/json"
        )
    )
    
    return httpClient.post("${GeminiConfig.BASE_URL}/models/${GeminiConfig.MODEL}:generateContent") {
        parameter("key", apiKey)
        contentType(ContentType.Application.Json)
        setBody(request)
    }.body<GeminiResponse>().toResumeAnalysis()
}
```

**Response Model:**

```kotlin
@Serializable
data class ResumeAnalysis(
    val matchScore: Int,
    val matchedKeywords: List<String>,
    val missingKeywords: List<String>,
    val suggestions: List<String>,
    val strengthAreas: List<String>,
    val improvementAreas: List<String>
)
```

### 2. Resume Optimization

Rewrite resume content to better match job requirements.

**Request:**

```kotlin
suspend fun optimizeResume(resume: String, jobDescription: String): OptimizedResume {
    val prompt = """
        Optimize the following resume for the given job description.
        
        RESUME:
        $resume
        
        JOB DESCRIPTION:
        $jobDescription
        
        Instructions:
        1. Enhance bullet points with metrics and impact
        2. Add relevant keywords from the job description
        3. Maintain truthfulness - don't fabricate experiences
        4. Use action verbs and quantifiable achievements
        
        Return JSON:
        {
            "optimizedSummary": "...",
            "optimizedExperience": [
                {
                    "title": "...",
                    "company": "...",
                    "bullets": ["...", "..."]
                }
            ],
            "addedKeywords": ["...", "..."],
            "changes": ["description of change 1", "..."]
        }
    """.trimIndent()
    
    // ... API call implementation
}
```

### 3. ATS Formatting Analysis

Analyze resume formatting for ATS compatibility.

**Request:**

```kotlin
suspend fun performATSAnalysis(
    resumeContent: String,
    jobDescription: String
): ATSAnalysis {
    val prompt = """
        Analyze the resume for ATS (Applicant Tracking System) compatibility.
        
        RESUME:
        $resumeContent
        
        JOB DESCRIPTION:
        $jobDescription
        
        Provide comprehensive ATS analysis as JSON:
        {
            "overallScore": <0-100>,
            "formatScore": <0-100>,
            "keywordScore": <0-100>,
            "readabilityScore": <0-100>,
            "sections": {
                "summary": { "score": <0-100>, "feedback": "...", "suggestions": ["..."] },
                "experience": { "score": <0-100>, "feedback": "...", "suggestions": ["..."] },
                "skills": { "score": <0-100>, "feedback": "...", "suggestions": ["..."] },
                "education": { "score": <0-100>, "feedback": "...", "suggestions": ["..."] }
            },
            "missingKeywords": {
                "hardSkills": ["..."],
                "softSkills": ["..."],
                "industryTerms": ["..."]
            },
            "formatIssues": ["..."],
            "impactBullets": [
                {
                    "original": "...",
                    "improved": "...",
                    "metrics": ["..."],
                    "confidence": <0.0-1.0>
                }
            ],
            "grammarIssues": [
                {
                    "text": "...",
                    "issue": "...",
                    "suggestion": "...",
                    "type": "GRAMMAR|PASSIVE_VOICE|WEAK_VERB|TONE"
                }
            ]
        }
    """.trimIndent()
    
    // ... API call implementation
}
```

**Response Model:**

```kotlin
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

@Serializable
data class ImpactBullet(
    val original: String,
    val improved: String,
    val metrics: List<String>,
    val confidence: Double
)

@Serializable
data class GrammarIssue(
    val text: String,
    val issue: String,
    val suggestion: String,
    val type: GrammarIssueType
)
```

### 4. Section-Specific Rewriting

Rewrite individual resume sections with targeted improvements.

**Request:**

```kotlin
suspend fun rewriteResumeSection(
    sectionType: SectionType,
    sectionContent: String,
    targetRole: String?,
    targetIndustry: String?,
    writingStyle: WritingStyle = WritingStyle.PROFESSIONAL
): SectionRewriteResult {
    val prompt = """
        Rewrite the following ${sectionType.name} section for a resume.
        
        CURRENT CONTENT:
        $sectionContent
        
        ${targetRole?.let { "TARGET ROLE: $it" } ?: ""}
        ${targetIndustry?.let { "TARGET INDUSTRY: $it" } ?: ""}
        WRITING STYLE: ${writingStyle.name}
        
        Provide rewritten section as JSON:
        {
            "rewrittenContent": "...",
            "changes": ["change description 1", "..."],
            "keywords": ["keyword1", "..."],
            "tips": ["improvement tip 1", "..."]
        }
    """.trimIndent()
    
    // ... API call implementation
}

enum class SectionType {
    SUMMARY,
    EXPERIENCE,
    SKILLS,
    EDUCATION
}

enum class WritingStyle {
    PROFESSIONAL,
    CONVERSATIONAL,
    TECHNICAL,
    EXECUTIVE
}
```

**Response Model:**

```kotlin
@Serializable
data class SectionRewriteResult(
    val rewrittenContent: String,
    val changes: List<String>,
    val keywords: List<String>,
    val tips: List<String>
)
```

### 5. Cover Letter Generation

Generate tailored cover letters based on resume and job description.

**Request:**

```kotlin
suspend fun generateCoverLetter(
    resume: String,
    jobDescription: String,
    companyInfo: String?,
    tone: CoverLetterTone = CoverLetterTone.PROFESSIONAL
): GeneratedCoverLetter {
    val prompt = """
        Generate a cover letter for the following position.
        
        RESUME:
        $resume
        
        JOB DESCRIPTION:
        $jobDescription
        
        ${companyInfo?.let { "COMPANY INFO: $it" } ?: ""}
        
        TONE: ${tone.name}
        
        Instructions:
        1. Highlight specific intersections between candidate skills and job requirements
        2. Include specific examples from the resume
        3. Show enthusiasm for the company and role
        4. Keep it concise (3-4 paragraphs)
        
        Return JSON:
        {
            "coverLetter": "...",
            "keyHighlights": ["...", "..."],
            "customizationPoints": ["...", "..."]
        }
    """.trimIndent()
    
    // ... API call implementation
}

enum class CoverLetterTone {
    PROFESSIONAL,
    ENTHUSIASTIC,
    FORMAL,
    CONVERSATIONAL
}
```

### 4. Mock Interview

Conduct AI-powered mock interviews with realistic questions.

**Streaming Request:**

```kotlin
suspend fun conductMockInterview(
    resume: String,
    jobDescription: String,
    previousResponses: List<InterviewExchange> = emptyList()
): Flow<String> = flow {
    val prompt = buildInterviewPrompt(resume, jobDescription, previousResponses)
    
    httpClient.preparePost("${GeminiConfig.BASE_URL}/models/${GeminiConfig.MODEL}:streamGenerateContent") {
        parameter("key", apiKey)
        parameter("alt", "sse")
        contentType(ContentType.Application.Json)
        setBody(createRequest(prompt))
    }.execute { response ->
        val channel = response.bodyAsChannel()
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break
            if (line.startsWith("data: ")) {
                val json = line.removePrefix("data: ")
                val chunk = Json.decodeFromString<StreamChunk>(json)
                chunk.candidates?.firstOrNull()?.content?.parts?.forEach {
                    emit(it.text)
                }
            }
        }
    }
}

private fun buildInterviewPrompt(
    resume: String,
    jobDescription: String,
    previousResponses: List<InterviewExchange>
): String {
    return """
        You are a tough but fair recruiter conducting a job interview.
        
        CANDIDATE RESUME:
        $resume
        
        JOB DESCRIPTION:
        $jobDescription
        
        ${if (previousResponses.isNotEmpty()) {
            "PREVIOUS CONVERSATION:\n" + previousResponses.joinToString("\n") { 
                "Q: ${it.question}\nA: ${it.answer}" 
            }
        } else ""}
        
        Instructions:
        1. Ask probing questions about experience gaps
        2. Challenge vague claims with requests for specifics
        3. Focus on behavioral questions (STAR method)
        4. Be realistic but not hostile
        
        Ask your next interview question:
    """.trimIndent()
}
```

### 5. STAR Response Coaching

Help users structure their interview responses.

**Request:**

```kotlin
suspend fun coachSTARResponse(
    question: String,
    draftResponse: String
): STARFeedback {
    val prompt = """
        Evaluate and coach the following interview response using the STAR method.
        
        QUESTION: $question
        
        CANDIDATE'S DRAFT RESPONSE:
        $draftResponse
        
        Analyze the response and provide:
        {
            "starBreakdown": {
                "situation": {
                    "present": true/false,
                    "content": "extracted or suggested content",
                    "feedback": "specific feedback"
                },
                "task": { ... },
                "action": { ... },
                "result": { ... }
            },
            "overallScore": <1-10>,
            "strengths": ["...", "..."],
            "improvements": ["...", "..."],
            "revisedResponse": "complete improved response"
        }
    """.trimIndent()
    
    // ... API call implementation
}
```

## Response Handling

### Generic Response Parser

```kotlin
@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    val error: GeminiError? = null
)

@Serializable
data class Candidate(
    val content: Content,
    val finishReason: String? = null,
    val safetyRatings: List<SafetyRating>? = null
)

@Serializable
data class GeminiError(
    val code: Int,
    val message: String,
    val status: String
)

inline fun <reified T> GeminiResponse.parseContent(): T {
    val text = candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        ?: throw ApiException("Empty response from API")
    return Json.decodeFromString<T>(text)
}
```

### Streaming Response Handler

```kotlin
@Serializable
data class StreamChunk(
    val candidates: List<StreamCandidate>? = null
)

@Serializable
data class StreamCandidate(
    val content: Content? = null,
    val finishReason: String? = null
)

class StreamingResponseCollector {
    private val buffer = StringBuilder()
    
    fun onChunk(chunk: String) {
        buffer.append(chunk)
        // Emit partial results for UI updates
    }
    
    fun getFullResponse(): String = buffer.toString()
}
```

## Error Handling

### Error Types

```kotlin
sealed class ApiError : Exception() {
    data class NetworkError(override val message: String) : ApiError()
    data class AuthenticationError(override val message: String) : ApiError()
    data class RateLimitError(val retryAfter: Int) : ApiError()
    data class ContentFilterError(val reason: String) : ApiError()
    data class ServerError(val code: Int, override val message: String) : ApiError()
    data class ParseError(override val message: String) : ApiError()
}
```

### Error Handler

```kotlin
suspend fun <T> safeApiCall(
    call: suspend () -> T
): Result<T> = try {
    Result.success(call())
} catch (e: ClientRequestException) {
    when (e.response.status.value) {
        401 -> Result.failure(ApiError.AuthenticationError("Invalid API key"))
        429 -> Result.failure(ApiError.RateLimitError(
            e.response.headers["Retry-After"]?.toIntOrNull() ?: 60
        ))
        else -> Result.failure(ApiError.ServerError(e.response.status.value, e.message))
    }
} catch (e: IOException) {
    Result.failure(ApiError.NetworkError("Network connection failed"))
} catch (e: SerializationException) {
    Result.failure(ApiError.ParseError("Failed to parse API response"))
}
```

### Retry Logic

```kotlin
suspend fun <T> withRetry(
    maxAttempts: Int = 3,
    initialDelay: Long = 1000,
    maxDelay: Long = 10000,
    factor: Double = 2.0,
    call: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxAttempts - 1) { attempt ->
        try {
            return call()
        } catch (e: ApiError.RateLimitError) {
            delay(e.retryAfter * 1000L)
        } catch (e: ApiError.NetworkError) {
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
    return call() // Final attempt
}
```

## Rate Limiting

### Rate Limiter Implementation

```kotlin
class RateLimiter(
    private val requestsPerMinute: Int = 60
) {
    private val requestTimestamps = mutableListOf<Long>()
    private val mutex = Mutex()
    
    suspend fun acquire() {
        mutex.withLock {
            val now = Clock.System.now().toEpochMilliseconds()
            val windowStart = now - 60_000
            
            // Remove old timestamps
            requestTimestamps.removeAll { it < windowStart }
            
            if (requestTimestamps.size >= requestsPerMinute) {
                val oldestInWindow = requestTimestamps.first()
                val waitTime = oldestInWindow + 60_000 - now
                delay(waitTime)
            }
            
            requestTimestamps.add(now)
        }
    }
}
```

## Caching

### Response Cache

```kotlin
class ResponseCache(
    private val database: VwaTekDatabase,
    private val maxAge: Duration = 24.hours
) {
    suspend fun getCached(key: String): String? {
        val cached = database.cacheQueries.get(key).executeAsOneOrNull()
        return if (cached != null && !isExpired(cached.timestamp)) {
            cached.response
        } else {
            null
        }
    }
    
    suspend fun cache(key: String, response: String) {
        database.cacheQueries.insert(
            key = key,
            response = response,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
    }
    
    private fun isExpired(timestamp: Long): Boolean {
        val age = Clock.System.now().toEpochMilliseconds() - timestamp
        return age > maxAge.inWholeMilliseconds
    }
}
```

## Testing

### Mock API Service

```kotlin
class MockGeminiApiService : GeminiApiService {
    var mockResponse: String = ""
    var shouldFail: Boolean = false
    var failureError: ApiError? = null
    
    override suspend fun generateContent(prompt: String): Result<String> {
        delay(100) // Simulate network delay
        
        return if (shouldFail) {
            Result.failure(failureError ?: ApiError.NetworkError("Mock error"))
        } else {
            Result.success(mockResponse)
        }
    }
}
```

### API Tests

```kotlin
class GeminiApiServiceTest {
    private lateinit var apiService: GeminiApiService
    private lateinit var mockEngine: MockEngine
    
    @BeforeTest
    fun setup() {
        mockEngine = MockEngine { request ->
            respond(
                content = mockResponseJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        apiService = GeminiApiServiceImpl(HttpClient(mockEngine), "test-key")
    }
    
    @Test
    fun `analyzeResume returns valid analysis`() = runTest {
        val result = apiService.analyzeResume("resume content", "job description")
        
        assertTrue(result.isSuccess)
        assertEquals(85, result.getOrNull()?.matchScore)
    }
}
```

## Best Practices

1. **Always use structured JSON responses** for reliable parsing
2. **Implement streaming** for long responses to improve UX
3. **Cache responses** when appropriate to reduce API costs
4. **Handle rate limits gracefully** with exponential backoff
5. **Validate and sanitize** all user inputs before sending to API
6. **Log API interactions** for debugging (exclude sensitive data)
7. **Use timeouts** to prevent hanging requests
8. **Test with mock services** for reliable unit tests

---

## Backend API (Phase 1)

The VwaTek backend is a Ktor server deployed on Google Cloud Run in the Canadian region (`northamerica-northeast1`).

### API Configuration

```kotlin
// shared/src/commonMain/kotlin/com/vwatek/apply/data/api/ApiConfig.kt
object ApiConfig {
    val BASE_URL = when (BuildKonfig.ENVIRONMENT) {
        "production" -> "https://api.vwatek.ca"
        "staging" -> "https://staging-api.vwatek.ca"
        else -> "http://localhost:8080"  // development
    }
    
    const val API_VERSION = "v1"
    val API_BASE = "$BASE_URL/api/$API_VERSION"
}
```

### Authentication Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/auth/register` | POST | Register new user |
| `/api/v1/auth/login` | POST | Authenticate user |
| `/api/v1/auth/logout` | POST | Invalidate session |
| `/api/v1/auth/refresh` | POST | Refresh JWT token |
| `/api/v1/auth/reset-password` | POST | Request password reset |

### Sync API

**Base Path:** `/api/v1/sync`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/sync/changes` | GET | Fetch server changes since timestamp |
| `/api/v1/sync/push` | POST | Push local changes to server |
| `/api/v1/sync/conflicts` | GET | Get unresolved conflicts |
| `/api/v1/sync/resolve` | POST | Resolve a sync conflict |
| `/api/v1/sync/status` | GET | Get sync status for user |

#### Sync Request/Response Models

```kotlin
@Serializable
data class SyncPushRequest(
    val operations: List<SyncOperation>,
    val deviceId: String,
    val lastSyncTimestamp: Long
)

@Serializable
data class SyncOperation(
    val id: String,
    val entityType: String,      // "resume", "cover_letter", "job_application"
    val entityId: String,
    val operationType: String,   // "create", "update", "delete"
    val data: String?,           // JSON payload for create/update
    val timestamp: Long,
    val deviceId: String
)

@Serializable
data class SyncPushResponse(
    val success: Boolean,
    val syncedOperations: List<String>,
    val conflicts: List<SyncConflict>,
    val serverTimestamp: Long
)

@Serializable
data class SyncConflict(
    val operationId: String,
    val entityType: String,
    val entityId: String,
    val localData: String,
    val serverData: String,
    val localTimestamp: Long,
    val serverTimestamp: Long
)
```

### Privacy API (PIPEDA Compliance)

**Base Path:** `/api/v1/privacy`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/privacy/consent` | GET | Get current consent preferences |
| `/api/v1/privacy/consent` | POST | Update consent preferences |
| `/api/v1/privacy/export` | POST | Request data export (PIPEDA right) |
| `/api/v1/privacy/export/{requestId}` | GET | Check export status |
| `/api/v1/privacy/export/{requestId}/download` | GET | Download exported data |
| `/api/v1/privacy/delete` | POST | Request account deletion |
| `/api/v1/privacy/delete/{requestId}` | GET | Check deletion status |

#### Consent Models

```kotlin
@Serializable
data class ConsentPreferences(
    val analyticsEnabled: Boolean,
    val dataSharingEnabled: Boolean,
    val marketingEnabled: Boolean,
    val consentVersion: String,
    val lastUpdated: Long,
    val ipAddress: String? = null
)

@Serializable
data class DataExportRequest(
    val format: String = "json",  // "json" or "csv"
    val includeResumes: Boolean = true,
    val includeCoverLetters: Boolean = true,
    val includeJobApplications: Boolean = true,
    val includeAnalytics: Boolean = false
)

@Serializable
data class DataExportStatus(
    val requestId: String,
    val status: String,          // "pending", "processing", "ready", "expired"
    val createdAt: Long,
    val expiresAt: Long?,
    val downloadUrl: String?
)
```

### Monitoring Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check (returns "OK") |
| `/metrics` | GET | Prometheus metrics |

---

### Job Tracker API (Phase 2)

**Base Path:** `/api/v1/jobs`  
**Authentication:** Required (JWT Bearer token)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/jobs` | GET | List applications (with filters: status, source, province, search) |
| `/api/v1/jobs` | POST | Create new application |
| `/api/v1/jobs/quick` | POST | Quick add application (minimal fields) |
| `/api/v1/jobs/{id}` | GET | Get application details with notes, reminders, interviews |
| `/api/v1/jobs/{id}` | PUT | Update application |
| `/api/v1/jobs/{id}` | DELETE | Delete application |
| `/api/v1/jobs/{id}/status` | PATCH | Update application status |
| `/api/v1/jobs/{id}/notes` | POST | Add note to application |
| `/api/v1/jobs/{id}/reminders` | POST | Add reminder to application |
| `/api/v1/jobs/{id}/interviews` | POST | Add interview to application |
| `/api/v1/jobs/stats` | GET | Get tracker statistics |
| `/api/v1/jobs/reminders/upcoming` | GET | Get upcoming reminders |

#### Job Tracker Models

```kotlin
@Serializable
data class JobApplicationDto(
    val id: String,
    val jobTitle: String,
    val companyName: String,
    val companyLogo: String? = null,
    val jobUrl: String? = null,
    val jobBoardSource: String? = null,  // LINKEDIN, INDEED, JOB_BANK_CANADA, etc.
    val city: String? = null,
    val province: String? = null,  // Canadian province code (ON, BC, QC, etc.)
    val isRemote: Boolean = false,
    val isHybrid: Boolean = false,
    val salaryMin: Int? = null,
    val salaryMax: Int? = null,
    val salaryCurrency: String? = "CAD",
    val salaryPeriod: String? = null,  // HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY
    val status: String,  // SAVED, APPLIED, PHONE_INTERVIEW, etc.
    val appliedAt: String? = null,
    val nocCode: String? = null,  // Canadian NOC code
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateJobApplicationDto(
    val jobTitle: String,
    val companyName: String,
    val resumeId: String? = null,
    val coverLetterId: String? = null,
    val companyLogo: String? = null,
    val jobUrl: String? = null,
    val jobDescription: String? = null,
    val jobBoardSource: String? = null,
    val externalJobId: String? = null,
    val city: String? = null,
    val province: String? = null,
    val country: String? = "Canada",
    val isRemote: Boolean = false,
    val isHybrid: Boolean = false,
    val salaryMin: Int? = null,
    val salaryMax: Int? = null,
    val salaryCurrency: String? = "CAD",
    val salaryPeriod: String? = null,
    val status: String? = "SAVED",
    val appliedAt: String? = null,
    val nocCode: String? = null,
    val requiresWorkPermit: Boolean? = null,
    val isLmiaRequired: Boolean? = null,
    val contactName: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null
)

@Serializable
data class TrackerStatsDto(
    val totalApplications: Int,
    val savedCount: Int,
    val appliedCount: Int,
    val interviewCount: Int,
    val offerCount: Int,
    val rejectedCount: Int,
    val interviewRate: Double,  // Percentage
    val offerRate: Double       // Percentage
)
```

#### Application Statuses

| Status | Description |
|--------|-------------|
| `SAVED` | Job saved for later |
| `APPLIED` | Application submitted |
| `PHONE_INTERVIEW` | Phone screen scheduled |
| `VIDEO_INTERVIEW` | Video interview scheduled |
| `TECHNICAL_INTERVIEW` | Technical assessment |
| `ONSITE_INTERVIEW` | On-site interview |
| `FINAL_INTERVIEW` | Final round interview |
| `OFFER` | Offer received |
| `NEGOTIATING` | In salary negotiation |
| `ACCEPTED` | Offer accepted |
| `REJECTED` | Application rejected |
| `WITHDRAWN` | Application withdrawn |
| `NO_RESPONSE` | No response received |

#### Job Board Sources

- `LINKEDIN` - LinkedIn Jobs
- `INDEED` - Indeed
- `GLASSDOOR` - Glassdoor
- `JOB_BANK_CANADA` - Job Bank Canada (Government)
- `MONSTER` - Monster
- `WORKDAY` - Workday
- `GREENHOUSE` - Greenhouse ATS
- `LEVER` - Lever ATS
- `COMPANY_WEBSITE` - Direct company website
- `REFERRAL` - Referred by someone
- `RECRUITER` - Recruiter outreach
- `OTHER` - Other sources

---

### Error Responses

All API errors follow a consistent format:

```kotlin
@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null
)
```

| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| 400 | `INVALID_REQUEST` | Malformed request body |
| 401 | `UNAUTHORIZED` | Missing or invalid JWT |
| 403 | `FORBIDDEN` | Insufficient permissions |
| 404 | `NOT_FOUND` | Resource not found |
| 409 | `CONFLICT` | Sync conflict detected |
| 429 | `RATE_LIMITED` | Too many requests |
| 500 | `INTERNAL_ERROR` | Server error |

### API Client Usage

```kotlin
// SyncApiClient usage
class SyncApiClient(private val httpClient: HttpClient) {
    suspend fun pushChanges(operations: List<SyncOperation>): Result<SyncPushResponse> {
        return try {
            val response = httpClient.post("${ApiConfig.API_BASE}/sync/push") {
                contentType(ContentType.Application.Json)
                setBody(SyncPushRequest(
                    operations = operations,
                    deviceId = PlatformUtils.getDeviceId(),
                    lastSyncTimestamp = getLastSyncTimestamp()
                ))
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// PrivacyApiClient usage
class PrivacyApiClient(private val httpClient: HttpClient) {
    suspend fun requestDataExport(request: DataExportRequest): Result<DataExportStatus> {
        return try {
            val response = httpClient.post("${ApiConfig.API_BASE}/privacy/export") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```
