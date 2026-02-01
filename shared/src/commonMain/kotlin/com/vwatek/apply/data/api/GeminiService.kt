package com.vwatek.apply.data.api

import com.vwatek.apply.domain.model.ResumeAnalysis
import com.vwatek.apply.domain.model.CoverLetter
import com.vwatek.apply.domain.model.CoverLetterTone
import com.vwatek.apply.domain.model.InterviewSession
import com.vwatek.apply.domain.model.InterviewQuestion
import com.vwatek.apply.domain.model.InterviewStatus
import com.vwatek.apply.domain.repository.SettingsRepository
import com.vwatek.apply.domain.usecase.StarResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class GeminiService(
    private val httpClient: HttpClient,
    private val settingsRepository: SettingsRepository
) {
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    @OptIn(ExperimentalUuidApi::class)
    suspend fun analyzeResume(resumeContent: String, jobDescription: String): ResumeAnalysis {
        val prompt = buildString {
            appendLine("You are an expert ATS (Applicant Tracking System) analyzer and career coach.")
            appendLine()
            appendLine("Analyze the following resume against the job description and provide:")
            appendLine("1. A match score from 0-100")
            appendLine("2. A list of missing keywords that should be added")
            appendLine("3. A list of specific recommendations to improve the resume")
            appendLine()
            appendLine("RESUME:")
            appendLine(resumeContent)
            appendLine()
            appendLine("JOB DESCRIPTION:")
            appendLine(jobDescription)
            appendLine()
            appendLine("Respond in JSON format:")
            appendLine("""{"matchScore": <number>, "missingKeywords": [<strings>], "recommendations": [<strings>]}""")
        }
        
        val response = callGemini(prompt)
        val parsed = parseAnalysisResponse(response)
        
        return ResumeAnalysis(
            id = Uuid.random().toString(),
            resumeId = "",
            jobDescription = jobDescription,
            matchScore = parsed.matchScore,
            missingKeywords = parsed.missingKeywords,
            recommendations = parsed.recommendations,
            createdAt = Clock.System.now()
        )
    }
    
    suspend fun optimizeResume(resumeContent: String, jobDescription: String): String {
        val prompt = buildString {
            appendLine("You are an expert resume writer and ATS optimization specialist.")
            appendLine()
            appendLine("Rewrite and optimize the following resume to better match the job description.")
            appendLine("Focus on:")
            appendLine("- Adding relevant keywords naturally")
            appendLine("- Converting passive language to active, metric-driven achievements")
            appendLine("- Improving readability and ATS compatibility")
            appendLine()
            appendLine("ORIGINAL RESUME:")
            appendLine(resumeContent)
            appendLine()
            appendLine("JOB DESCRIPTION:")
            appendLine(jobDescription)
            appendLine()
            appendLine("Provide the optimized resume content only, no explanations.")
        }
        
        return callGemini(prompt)
    }
    
    @OptIn(ExperimentalUuidApi::class)
    suspend fun generateCoverLetter(
        resumeContent: String,
        jobTitle: String,
        companyName: String,
        jobDescription: String,
        tone: CoverLetterTone
    ): CoverLetter {
        val toneDescription = when (tone) {
            CoverLetterTone.PROFESSIONAL -> "professional and polished"
            CoverLetterTone.ENTHUSIASTIC -> "enthusiastic and energetic"
            CoverLetterTone.FORMAL -> "formal and traditional"
            CoverLetterTone.CREATIVE -> "creative and unique"
        }
        
        val prompt = buildString {
            appendLine("You are an expert cover letter writer.")
            appendLine()
            appendLine("Write a ${toneDescription} cover letter for the following position.")
            appendLine("The letter should:")
            appendLine("- Be tailored to the specific company and role")
            appendLine("- Highlight relevant experience from the resume")
            appendLine("- Show genuine interest in the company")
            appendLine("- Be concise (3-4 paragraphs)")
            appendLine()
            appendLine("RESUME:")
            appendLine(resumeContent)
            appendLine()
            appendLine("JOB TITLE: $jobTitle")
            appendLine("COMPANY: $companyName")
            appendLine()
            appendLine("JOB DESCRIPTION:")
            appendLine(jobDescription)
            appendLine()
            appendLine("Write the cover letter content only, no salutation or signature placeholders.")
        }
        
        val content = callGemini(prompt)
        
        return CoverLetter(
            id = Uuid.random().toString(),
            resumeId = null,
            jobTitle = jobTitle,
            companyName = companyName,
            content = content,
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
        val prompt = buildString {
            appendLine("You are a tough but fair technical recruiter conducting a job interview.")
            appendLine()
            appendLine("Generate 5 challenging interview questions for the following position.")
            appendLine("Include a mix of:")
            appendLine("- Behavioral questions (STAR format)")
            appendLine("- Technical/skill-based questions")
            appendLine("- Situational questions")
            appendLine()
            appendLine("JOB TITLE: $jobTitle")
            appendLine()
            appendLine("JOB DESCRIPTION:")
            appendLine(jobDescription)
            if (resumeContent != null) {
                appendLine()
                appendLine("CANDIDATE RESUME:")
                appendLine(resumeContent)
            }
            appendLine()
            appendLine("Respond with only the questions, numbered 1-5, one per line.")
        }
        
        val response = callGemini(prompt)
        val questions = parseInterviewQuestions(response)
        
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
        val prompt = buildString {
            appendLine("You are an expert interview coach.")
            appendLine()
            appendLine("Evaluate the following interview answer for a $jobTitle position.")
            appendLine("Provide constructive feedback on:")
            appendLine("- Clarity and structure")
            appendLine("- Use of specific examples")
            appendLine("- Relevance to the question")
            appendLine("- Areas for improvement")
            appendLine()
            appendLine("QUESTION: $question")
            appendLine()
            appendLine("ANSWER: $answer")
            appendLine()
            appendLine("Provide brief, actionable feedback (2-3 sentences).")
        }
        
        return callGemini(prompt)
    }
    
    suspend fun getStarCoaching(experience: String, jobContext: String): StarResponse {
        val prompt = buildString {
            appendLine("You are a STAR method interview coaching expert.")
            appendLine()
            appendLine("Help structure the following experience using the STAR method.")
            appendLine()
            appendLine("EXPERIENCE: $experience")
            appendLine()
            appendLine("JOB CONTEXT: $jobContext")
            appendLine()
            appendLine("Respond in JSON format:")
            appendLine("""{"situation": "<text>", "task": "<text>", "action": "<text>", "result": "<text>", "suggestions": [<improvement tips>]}""")
        }
        
        val response = callGemini(prompt)
        return parseStarResponse(response)
    }
    
    private suspend fun callGemini(prompt: String): String {
        val apiKey = settingsRepository.getSetting("gemini_api_key")
        val openAiKey = settingsRepository.getSetting("openai_api_key")
        
        // Try Gemini first if API key is available
        if (!apiKey.isNullOrBlank()) {
            try {
                return callGeminiApi(prompt, apiKey)
            } catch (e: Exception) {
                // If Gemini fails and we have OpenAI key, try fallback
                if (!openAiKey.isNullOrBlank()) {
                    println("Gemini API failed, falling back to OpenAI: ${e.message}")
                    return callOpenAiApi(prompt, openAiKey)
                }
                throw e
            }
        }
        
        // If no Gemini key but we have OpenAI key, use OpenAI
        if (!openAiKey.isNullOrBlank()) {
            return callOpenAiApi(prompt, openAiKey)
        }
        
        throw IllegalStateException("No AI API key configured. Please add your Gemini or OpenAI API key in Settings.")
    }
    
    private suspend fun callGeminiApi(prompt: String, apiKey: String): String {
        val requestBody = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            )
        )
        
        val response: GeminiResponse = httpClient.post("$baseUrl?key=$apiKey") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.body()
        
        return response.candidates.firstOrNull()
            ?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("No response from Gemini")
    }
    
    private suspend fun callOpenAiApi(prompt: String, apiKey: String): String {
        val openAiUrl = "https://api.openai.com/v1/chat/completions"
        
        val requestBody = OpenAiRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                OpenAiMessage(role = "user", content = prompt)
            ),
            temperature = 0.7
        )
        
        val response: OpenAiResponse = httpClient.post(openAiUrl) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            setBody(requestBody)
        }.body()
        
        return response.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("No response from OpenAI")
    }
    
    private fun parseAnalysisResponse(response: String): AnalysisData {
        val jsonString = extractJson(response)
        return try {
            json.decodeFromString<AnalysisData>(jsonString)
        } catch (e: Exception) {
            AnalysisData(
                matchScore = 50,
                missingKeywords = emptyList(),
                recommendations = listOf("Unable to parse response. Please try again.")
            )
        }
    }
    
    private fun parseInterviewQuestions(response: String): List<String> {
        return response.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                line.removePrefix("1.").removePrefix("2.").removePrefix("3.")
                    .removePrefix("4.").removePrefix("5.").trim()
            }
            .filter { it.isNotEmpty() }
            .take(5)
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
    
    private fun extractJson(text: String): String {
        val startIndex = text.indexOf('{')
        val endIndex = text.lastIndexOf('}')
        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            text.substring(startIndex, endIndex + 1)
        } else {
            text
        }
    }
}

@Serializable
private data class GeminiRequest(
    val contents: List<GeminiContent>
)

@Serializable
private data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
private data class GeminiPart(
    val text: String
)

@Serializable
private data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

@Serializable
private data class GeminiCandidate(
    val content: GeminiContent? = null
)

@Serializable
private data class AnalysisData(
    val matchScore: Int,
    val missingKeywords: List<String>,
    val recommendations: List<String>
)

@Serializable
private data class StarData(
    val situation: String,
    val task: String,
    val action: String,
    val result: String,
    val suggestions: List<String>
)

// OpenAI API data classes for fallback
@Serializable
private data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.7
)

@Serializable
private data class OpenAiMessage(
    val role: String,
    val content: String
)

@Serializable
private data class OpenAiResponse(
    val choices: List<OpenAiChoice> = emptyList()
)

@Serializable
private data class OpenAiChoice(
    val message: OpenAiMessage? = null
)
