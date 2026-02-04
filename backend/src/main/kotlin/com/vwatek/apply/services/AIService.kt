package com.vwatek.apply.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AIService(private val httpClient: HttpClient) {
    
    private val geminiApiKey = System.getenv("GEMINI_API_KEY") ?: ""
    private val openAiApiKey = System.getenv("OPENAI_API_KEY") ?: ""
    
    private val geminiBaseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
    private val openAiBaseUrl = "https://api.openai.com/v1/chat/completions"
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    suspend fun generateContent(prompt: String): String {
        // Try Gemini first
        if (geminiApiKey.isNotBlank()) {
            try {
                return callGemini(prompt)
            } catch (e: Exception) {
                println("Gemini API failed: ${e.message}")
                // Fall through to OpenAI
            }
        }
        
        // Fallback to OpenAI
        if (openAiApiKey.isNotBlank()) {
            return callOpenAi(prompt)
        }
        
        throw IllegalStateException("No AI API keys configured on server")
    }
    
    private suspend fun callGemini(prompt: String): String {
        val requestBody = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            )
        )
        
        val response: HttpResponse = httpClient.post("$geminiBaseUrl?key=$geminiApiKey") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        
        val geminiResponse: GeminiResponse = response.body()
        return geminiResponse.candidates.firstOrNull()
            ?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("No response from Gemini")
    }
    
    private suspend fun callOpenAi(prompt: String): String {
        val requestBody = OpenAiRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                OpenAiMessage(role = "user", content = prompt)
            ),
            temperature = 0.7
        )
        
        val response: HttpResponse = httpClient.post(openAiBaseUrl) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $openAiApiKey")
            setBody(requestBody)
        }
        
        val openAiResponse: OpenAiResponse = response.body()
        return openAiResponse.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("No response from OpenAI")
    }
    
    suspend fun analyzeResume(resumeContent: String, jobDescription: String): ResumeAnalysisResult {
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
        
        val response = generateContent(prompt)
        return parseAnalysisResponse(response)
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
        
        return generateContent(prompt)
    }
    
    suspend fun generateCoverLetter(
        resumeContent: String,
        jobTitle: String,
        companyName: String,
        jobDescription: String,
        tone: String
    ): String {
        val toneDescription = when (tone.uppercase()) {
            "PROFESSIONAL" -> "professional and polished"
            "ENTHUSIASTIC" -> "enthusiastic and energetic"
            "FORMAL" -> "formal and traditional"
            "CREATIVE" -> "creative and unique"
            else -> "professional and polished"
        }
        
        val prompt = buildString {
            appendLine("You are an expert cover letter writer.")
            appendLine()
            appendLine("Write a $toneDescription cover letter for the following position.")
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
        
        return generateContent(prompt)
    }
    
    suspend fun generateInterviewQuestions(
        resumeContent: String?,
        jobTitle: String,
        jobDescription: String
    ): List<String> {
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
        
        val response = generateContent(prompt)
        return parseInterviewQuestions(response)
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
        
        return generateContent(prompt)
    }
    
    private fun parseAnalysisResponse(response: String): ResumeAnalysisResult {
        val jsonString = extractJson(response)
        return try {
            json.decodeFromString<ResumeAnalysisResult>(jsonString)
        } catch (e: Exception) {
            ResumeAnalysisResult(
                matchScore = 50,
                missingKeywords = emptyList(),
                recommendations = listOf("Unable to parse response. Please try again.")
            )
        }
    }
    
    private fun parseInterviewQuestions(response: String): List<String> {
        return response.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                // Remove numbering like "1.", "1)", "1:"
                line.replace(Regex("^\\d+[.):]\\s*"), "")
            }
            .filter { it.isNotBlank() }
            .take(5)
    }
    
    private fun extractJson(text: String): String {
        // Find JSON object in the response
        val startIndex = text.indexOf('{')
        val endIndex = text.lastIndexOf('}')
        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            text.substring(startIndex, endIndex + 1)
        } else {
            text
        }
    }
}

// Request/Response models for Gemini
@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null
)

// Request/Response models for OpenAI
@Serializable
data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.7
)

@Serializable
data class OpenAiMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAiResponse(
    val choices: List<OpenAiChoice> = emptyList()
)

@Serializable
data class OpenAiChoice(
    val message: OpenAiMessage
)

// Analysis result model
@Serializable
data class ResumeAnalysisResult(
    val matchScore: Int,
    val missingKeywords: List<String>,
    val recommendations: List<String>
)
