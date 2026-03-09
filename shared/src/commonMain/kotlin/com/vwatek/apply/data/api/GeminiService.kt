package com.vwatek.apply.data.api

import com.vwatek.apply.domain.model.ResumeAnalysis
import com.vwatek.apply.domain.model.CoverLetter
import com.vwatek.apply.domain.model.CoverLetterTone
import com.vwatek.apply.domain.model.InterviewSession
import com.vwatek.apply.domain.model.InterviewQuestion
import com.vwatek.apply.domain.model.InterviewStatus
import com.vwatek.apply.domain.repository.SettingsRepository
import com.vwatek.apply.domain.usecase.StarResponse
import com.vwatek.apply.domain.usecase.SectionRewriteResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class GeminiService(
    private val httpClient: HttpClient,
    @Suppress("unused") private val settingsRepository: SettingsRepository
) {
    // All AI calls go through the backend API which manages API keys server-side
    private val backendUrl: String
        get() = ApiConfig.apiV1Url
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    @OptIn(ExperimentalUuidApi::class)
    suspend fun analyzeResume(resumeContent: String, jobDescription: String): ResumeAnalysis {
        val response: BackendAnalyzeResponse = httpClient.post("$backendUrl/ai/analyze-resume") {
            contentType(ContentType.Application.Json)
            setBody(BackendAnalyzeRequest(resumeContent = resumeContent, jobDescription = jobDescription))
        }.body()
        
        return ResumeAnalysis(
            id = Uuid.random().toString(),
            resumeId = "",
            jobDescription = jobDescription,
            matchScore = response.matchScore,
            missingKeywords = response.missingKeywords,
            recommendations = response.recommendations,
            createdAt = Clock.System.now()
        )
    }
    
    suspend fun optimizeResume(resumeContent: String, jobDescription: String): String {
        val response: BackendOptimizeResponse = httpClient.post("$backendUrl/ai/optimize-resume") {
            contentType(ContentType.Application.Json)
            setBody(BackendOptimizeRequest(resumeContent = resumeContent, jobDescription = jobDescription))
        }.body()
        
        return response.optimizedContent
    }
    
    @OptIn(ExperimentalUuidApi::class)
    suspend fun generateCoverLetter(
        resumeContent: String,
        jobTitle: String,
        companyName: String,
        jobDescription: String,
        tone: CoverLetterTone
    ): CoverLetter {
        val response: BackendCoverLetterResponse = httpClient.post("$backendUrl/ai/generate-cover-letter") {
            contentType(ContentType.Application.Json)
            setBody(BackendCoverLetterRequest(
                resumeContent = resumeContent,
                jobTitle = jobTitle,
                companyName = companyName,
                jobDescription = jobDescription,
                tone = tone.name
            ))
        }.body()
        
        return CoverLetter(
            id = Uuid.random().toString(),
            resumeId = null,
            jobTitle = jobTitle,
            companyName = companyName,
            content = response.content,
            tone = tone,
            createdAt = Clock.System.now()
        )
    }
    
    @OptIn(ExperimentalUuidApi::class)
    suspend fun startMockInterview(
        resumeContent: String?,
        jobTitle: String,
        jobDescription: String
    ): InterviewSession {
        val response: BackendInterviewQuestionsResponse = httpClient.post("$backendUrl/ai/generate-interview-questions") {
            contentType(ContentType.Application.Json)
            setBody(BackendInterviewQuestionsRequest(
                resumeContent = resumeContent,
                jobTitle = jobTitle,
                jobDescription = jobDescription
            ))
        }.body()
        
        val questions = response.questions
        val sessionId = Uuid.random().toString()
        val now = Clock.System.now()
        
        val interviewQuestions = questions.mapIndexed { index, questionText ->
            InterviewQuestion(
                id = Uuid.random().toString(),
                sessionId = sessionId,
                question = questionText,
                userAnswer = null,
                aiFeedback = null,
                questionOrder = index,
                createdAt = now
            )
        }
        
        return InterviewSession(
            id = sessionId,
            resumeId = null,
            jobTitle = jobTitle,
            jobDescription = jobDescription,
            status = InterviewStatus.IN_PROGRESS,
            questions = interviewQuestions,
            createdAt = now,
            completedAt = null
        )
    }
    
    suspend fun getInterviewFeedback(
        question: String,
        answer: String,
        jobTitle: String
    ): String {
        val response: BackendInterviewFeedbackResponse = httpClient.post("$backendUrl/ai/interview-feedback") {
            contentType(ContentType.Application.Json)
            setBody(BackendInterviewFeedbackRequest(
                question = question,
                answer = answer,
                jobTitle = jobTitle
            ))
        }.body()
        
        return response.feedback
    }
    
    suspend fun getStarCoaching(experience: String, jobContext: String): StarResponse {
        val response: BackendAITextResponse = httpClient.post("$backendUrl/ai/star-coaching") {
            contentType(ContentType.Application.Json)
            setBody(BackendStarCoachingRequest(experience = experience, jobContext = jobContext))
        }.body()
        
        return parseStarResponse(response.result)
    }
    
    @OptIn(ExperimentalUuidApi::class)
    suspend fun performATSAnalysis(resumeContent: String, jobDescription: String?): ATSAnalysisResult {
        val response: BackendAITextResponse = httpClient.post("$backendUrl/ai/ats-analysis") {
            contentType(ContentType.Application.Json)
            setBody(BackendATSAnalysisRequest(resumeContent = resumeContent, jobDescription = jobDescription))
        }.body()
        
        return parseATSAnalysisResponse(response.result)
    }
    
    suspend fun generateImpactBullets(experiences: List<String>, jobContext: String): List<ImpactBulletResult> {
        val response: BackendAITextResponse = httpClient.post("$backendUrl/ai/impact-bullets") {
            contentType(ContentType.Application.Json)
            setBody(BackendImpactBulletsRequest(experiences = experiences, jobContext = jobContext))
        }.body()
        
        return parseImpactBulletsResponse(response.result)
    }
    
    suspend fun analyzeGrammarAndTone(text: String): List<GrammarResult> {
        val response: BackendAITextResponse = httpClient.post("$backendUrl/ai/grammar-analysis") {
            contentType(ContentType.Application.Json)
            setBody(BackendGrammarAnalysisRequest(text = text))
        }.body()
        
        return parseGrammarResponse(response.result)
    }
    
    suspend fun rewriteResumeSection(
        sectionType: String,
        sectionContent: String,
        targetRole: String?,
        targetIndustry: String?,
        style: String = "professional"
    ): SectionRewriteResult {
        val response: BackendAITextResponse = httpClient.post("$backendUrl/ai/rewrite-section") {
            contentType(ContentType.Application.Json)
            setBody(BackendRewriteSectionRequest(
                sectionType = sectionType,
                sectionContent = sectionContent,
                targetRole = targetRole,
                targetIndustry = targetIndustry,
                style = style
            ))
        }.body()
        
        return parseSectionRewriteResponse(response.result)
    }
    
    private fun parseSectionRewriteResponse(response: String): SectionRewriteResult {
        val jsonString = extractJson(response)
        return try {
            json.decodeFromString<SectionRewriteResult>(jsonString)
        } catch (e: Exception) {
            SectionRewriteResult(
                rewrittenContent = "",
                changes = listOf("Unable to parse response. Please try again."),
                keywords = emptyList(),
                tips = emptyList()
            )
        }
    }

    private fun parseStarResponse(response: String): StarResponse {
        val jsonString = extractJson(response)
        return try {
            val data = json.decodeFromString<StarData>(jsonString)
            StarResponse(
                situation = data.situation,
                task = data.task,
                action = data.action,
                result = data.result,
                suggestions = data.suggestions
            )
        } catch (e: Exception) {
            StarResponse(
                situation = "",
                task = "",
                action = "",
                result = "",
                suggestions = listOf("Unable to parse response. Please try again.")
            )
        }
    }
    
    private fun parseATSAnalysisResponse(response: String): ATSAnalysisResult {
        val jsonString = extractJson(response)
        return try {
            json.decodeFromString<ATSAnalysisResult>(jsonString)
        } catch (e: Exception) {
            ATSAnalysisResult(
                overallScore = 50,
                formattingScore = 50,
                keywordScore = 50,
                structureScore = 50,
                readabilityScore = 50,
                formattingIssues = emptyList(),
                structureIssues = emptyList(),
                keywordDensity = emptyMap(),
                recommendations = emptyList(),
                impactBullets = emptyList(),
                grammarIssues = emptyList()
            )
        }
    }
    
    private fun parseImpactBulletsResponse(response: String): List<ImpactBulletResult> {
        val jsonString = extractJsonArray(response)
        return try {
            json.decodeFromString<List<ImpactBulletResult>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun parseGrammarResponse(response: String): List<GrammarResult> {
        val jsonString = extractJsonArray(response)
        return try {
            json.decodeFromString<List<GrammarResult>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun extractJson(text: String): String {
        val startIndex = text.indexOf('{')
        val endIndex = text.lastIndexOf('}')
        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            text.substring(startIndex, endIndex + 1)
        } else {
            text
        }
    }
    
    private fun extractJsonArray(text: String): String {
        val startIndex = text.indexOf('[')
        val endIndex = text.lastIndexOf(']')
        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            text.substring(startIndex, endIndex + 1)
        } else {
            text
        }
    }
}

@Serializable
private data class StarData(
    val situation: String,
    val task: String,
    val action: String,
    val result: String,
    val suggestions: List<String>
)

// ATS Analysis response data classes
@Serializable
data class ATSAnalysisResult(
    val overallScore: Int,
    val formattingScore: Int,
    val keywordScore: Int,
    val structureScore: Int,
    val readabilityScore: Int,
    val formattingIssues: List<ATSIssueData> = emptyList(),
    val structureIssues: List<ATSIssueData> = emptyList(),
    val keywordDensity: Map<String, Int> = emptyMap(),
    val recommendations: List<ATSRecommendationData> = emptyList(),
    val impactBullets: List<ImpactBulletData> = emptyList(),
    val grammarIssues: List<GrammarIssueData> = emptyList()
)

@Serializable
data class ATSIssueData(
    val severity: String,
    val category: String,
    val description: String,
    val suggestion: String
)

@Serializable
data class ATSRecommendationData(
    val priority: Int,
    val category: String,
    val title: String,
    val description: String,
    val impact: String
)

@Serializable
data class ImpactBulletData(
    val original: String,
    val improved: String,
    val xyzFormat: XYZFormatData? = null
)

@Serializable
data class XYZFormatData(
    val accomplished: String,
    val measuredBy: String,
    val byDoing: String
)

@Serializable
data class GrammarIssueData(
    val original: String,
    val corrected: String,
    val explanation: String,
    val type: String
)

@Serializable
data class ImpactBulletResult(
    val original: String,
    val improved: String,
    val accomplished: String = "",
    val measuredBy: String = "",
    val byDoing: String = ""
)

@Serializable
data class GrammarResult(
    val original: String,
    val corrected: String,
    val explanation: String,
    val type: String
)

// Backend API request/response models
@Serializable
private data class BackendAnalyzeRequest(
    val resumeContent: String,
    val jobDescription: String
)

@Serializable
private data class BackendAnalyzeResponse(
    val matchScore: Int,
    val missingKeywords: List<String>,
    val recommendations: List<String>
)

@Serializable
private data class BackendOptimizeRequest(
    val resumeContent: String,
    val jobDescription: String
)

@Serializable
private data class BackendOptimizeResponse(
    val optimizedContent: String
)

@Serializable
private data class BackendCoverLetterRequest(
    val resumeContent: String,
    val jobTitle: String,
    val companyName: String,
    val jobDescription: String,
    val tone: String = "PROFESSIONAL"
)

@Serializable
private data class BackendCoverLetterResponse(
    val content: String
)

@Serializable
private data class BackendInterviewQuestionsRequest(
    val resumeContent: String? = null,
    val jobTitle: String,
    val jobDescription: String
)

@Serializable
private data class BackendInterviewQuestionsResponse(
    val questions: List<String>
)

@Serializable
private data class BackendInterviewFeedbackRequest(
    val question: String,
    val answer: String,
    val jobTitle: String
)

@Serializable
private data class BackendInterviewFeedbackResponse(
    val feedback: String
)

@Serializable
private data class BackendAITextResponse(
    val result: String
)

@Serializable
private data class BackendStarCoachingRequest(
    val experience: String,
    val jobContext: String
)

@Serializable
private data class BackendATSAnalysisRequest(
    val resumeContent: String,
    val jobDescription: String? = null
)

@Serializable
private data class BackendImpactBulletsRequest(
    val experiences: List<String>,
    val jobContext: String
)

@Serializable
private data class BackendGrammarAnalysisRequest(
    val text: String
)

@Serializable
private data class BackendRewriteSectionRequest(
    val sectionType: String,
    val sectionContent: String,
    val targetRole: String? = null,
    val targetIndustry: String? = null,
    val style: String = "professional"
)
