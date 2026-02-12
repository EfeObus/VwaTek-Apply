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
    private val geminiBaseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
    
    // Backend API URL - using centralized ApiConfig (Canadian region in production)
    private val backendUrl: String
        get() = ApiConfig.apiV1Url
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    @OptIn(ExperimentalUuidApi::class)
    suspend fun analyzeResume(resumeContent: String, jobDescription: String): ResumeAnalysis {
        // Try backend API first (uses centralized API keys)
        try {
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
        } catch (e: Exception) {
            println("Backend analyze failed: ${e.message}, trying local...")
        }
        
        // Fallback to local API call
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
        // Try backend API first (uses centralized API keys)
        try {
            val response: BackendOptimizeResponse = httpClient.post("$backendUrl/ai/optimize-resume") {
                contentType(ContentType.Application.Json)
                setBody(BackendOptimizeRequest(resumeContent = resumeContent, jobDescription = jobDescription))
            }.body()
            
            return response.optimizedContent
        } catch (e: Exception) {
            println("Backend optimize failed: ${e.message}, trying local...")
        }
        
        // Fallback to local API call
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
        // Try backend API first (uses centralized API keys)
        try {
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
        } catch (e: Exception) {
            println("Backend cover letter failed: ${e.message}, trying local...")
        }
        
        // Fallback to local API call
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
        var questions: List<String>
        
        // Try backend API first (uses centralized API keys)
        try {
            val response: BackendInterviewQuestionsResponse = httpClient.post("$backendUrl/ai/generate-interview-questions") {
                contentType(ContentType.Application.Json)
                setBody(BackendInterviewQuestionsRequest(
                    resumeContent = resumeContent,
                    jobTitle = jobTitle,
                    jobDescription = jobDescription
                ))
            }.body()
            
            questions = response.questions
        } catch (e: Exception) {
            println("Backend interview questions failed: ${e.message}, trying local...")
            
            // Fallback to local API call
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
            questions = parseInterviewQuestions(response)
        }
        
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
        // Try backend API first (uses centralized API keys)
        try {
            val response: BackendInterviewFeedbackResponse = httpClient.post("$backendUrl/ai/interview-feedback") {
                contentType(ContentType.Application.Json)
                setBody(BackendInterviewFeedbackRequest(
                    question = question,
                    answer = answer,
                    jobTitle = jobTitle
                ))
            }.body()
            
            return response.feedback
        } catch (e: Exception) {
            println("Backend interview feedback failed: ${e.message}, trying local...")
        }
        
        // Fallback to local API call
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
    
    @OptIn(ExperimentalUuidApi::class)
    suspend fun performATSAnalysis(resumeContent: String, jobDescription: String?): ATSAnalysisResult {
        val prompt = buildString {
            appendLine("You are an expert ATS (Applicant Tracking System) analyst and resume optimization specialist.")
            appendLine()
            appendLine("Perform a comprehensive ATS compatibility analysis on the following resume.")
            appendLine("Evaluate:")
            appendLine("1. FORMATTING: Check for ATS-unfriendly elements (tables, columns, headers/footers, images, special characters)")
            appendLine("2. STRUCTURE: Verify proper section organization (Contact, Summary, Experience, Education, Skills)")
            appendLine("3. KEYWORDS: Analyze keyword density and relevance")
            appendLine("4. READABILITY: Check for clear, scannable content")
            appendLine("5. IMPACT: Identify bullet points that could use X-Y-Z format (Accomplished X as measured by Y, by doing Z)")
            appendLine("6. GRAMMAR & TONE: Check for professional language, grammar issues, and consistency")
            appendLine()
            appendLine("RESUME:")
            appendLine(resumeContent)
            if (jobDescription != null) {
                appendLine()
                appendLine("TARGET JOB DESCRIPTION (for keyword matching):")
                appendLine(jobDescription)
            }
            appendLine()
            appendLine("Respond ONLY with valid JSON in this exact format:")
            appendLine("""{
  "overallScore": <0-100>,
  "formattingScore": <0-100>,
  "keywordScore": <0-100>,
  "structureScore": <0-100>,
  "readabilityScore": <0-100>,
  "formattingIssues": [
    {"severity": "HIGH|MEDIUM|LOW", "category": "<category>", "description": "<issue>", "suggestion": "<fix>"}
  ],
  "structureIssues": [
    {"severity": "HIGH|MEDIUM|LOW", "category": "<category>", "description": "<issue>", "suggestion": "<fix>"}
  ],
  "keywordDensity": {"<keyword>": <count>},
  "recommendations": [
    {"priority": <1-5>, "category": "<category>", "title": "<title>", "description": "<details>", "impact": "<expected improvement>"}
  ],
  "impactBullets": [
    {"original": "<current bullet>", "improved": "<rewritten with metrics>", "xyzFormat": {"accomplished": "<X>", "measuredBy": "<Y>", "byDoing": "<Z>"}}
  ],
  "grammarIssues": [
    {"original": "<text>", "corrected": "<fixed text>", "explanation": "<why>", "type": "GRAMMAR|SPELLING|TONE|CLARITY|REDUNDANCY"}
  ]
}""")
        }
        
        val response = callGemini(prompt)
        return parseATSAnalysisResponse(response)
    }
    
    suspend fun generateImpactBullets(experiences: List<String>, jobContext: String): List<ImpactBulletResult> {
        val prompt = buildString {
            appendLine("You are an expert resume writer specializing in high-impact achievement statements.")
            appendLine()
            appendLine("Transform the following experience bullet points into powerful X-Y-Z format statements:")
            appendLine("'Accomplished [X] as measured by [Y], by doing [Z]'")
            appendLine()
            appendLine("JOB CONTEXT: $jobContext")
            appendLine()
            appendLine("BULLET POINTS TO IMPROVE:")
            experiences.forEachIndexed { index, exp ->
                appendLine("${index + 1}. $exp")
            }
            appendLine()
            appendLine("Respond ONLY with valid JSON array:")
            appendLine("""[{"original": "<original text>", "improved": "<X-Y-Z format>", "accomplished": "<X>", "measuredBy": "<Y>", "byDoing": "<Z>"}]""")
        }
        
        val response = callGemini(prompt)
        return parseImpactBulletsResponse(response)
    }
    
    suspend fun analyzeGrammarAndTone(text: String): List<GrammarResult> {
        val prompt = buildString {
            appendLine("You are a professional editor specializing in resume and business writing.")
            appendLine()
            appendLine("Analyze the following text for grammar, spelling, tone, and clarity issues.")
            appendLine("Focus on:")
            appendLine("- Grammar and punctuation errors")
            appendLine("- Spelling mistakes")
            appendLine("- Passive voice (should be active)")
            appendLine("- Weak verbs (should use strong action verbs)")
            appendLine("- Redundant phrases")
            appendLine("- Unprofessional tone")
            appendLine()
            appendLine("TEXT:")
            appendLine(text)
            appendLine()
            appendLine("Respond ONLY with valid JSON array:")
            appendLine("""[{"original": "<problematic text>", "corrected": "<fixed version>", "explanation": "<why this change>", "type": "GRAMMAR|SPELLING|TONE|CLARITY|REDUNDANCY"}]""")
        }
        
        val response = callGemini(prompt)
        return parseGrammarResponse(response)
    }
    
    suspend fun rewriteResumeSection(
        sectionType: String,
        sectionContent: String,
        targetRole: String?,
        targetIndustry: String?,
        style: String = "professional"
    ): SectionRewriteResult {
        val prompt = buildString {
            appendLine("You are an expert resume writer and career coach with 15+ years of experience.")
            appendLine()
            appendLine("Rewrite the following resume section to be more impactful and ATS-optimized.")
            appendLine()
            appendLine("SECTION TYPE: $sectionType")
            if (!targetRole.isNullOrBlank()) appendLine("TARGET ROLE: $targetRole")
            if (!targetIndustry.isNullOrBlank()) appendLine("TARGET INDUSTRY: $targetIndustry")
            appendLine("WRITING STYLE: $style")
            appendLine()
            appendLine("ORIGINAL CONTENT:")
            appendLine(sectionContent)
            appendLine()
            appendLine("Guidelines for rewriting:")
            when (sectionType.uppercase()) {
                "SUMMARY", "PROFILE", "OBJECTIVE" -> {
                    appendLine("- Write a compelling professional summary (3-5 sentences)")
                    appendLine("- Start with your professional title and years of experience")
                    appendLine("- Highlight 2-3 key achievements with metrics")
                    appendLine("- Include relevant skills and expertise areas")
                    appendLine("- End with your career objective or value proposition")
                }
                "EXPERIENCE", "WORK EXPERIENCE", "EMPLOYMENT" -> {
                    appendLine("- Use strong action verbs at the start of each bullet point")
                    appendLine("- Follow the X-Y-Z format: Accomplished [X] as measured by [Y] by doing [Z]")
                    appendLine("- Include specific metrics and quantifiable achievements")
                    appendLine("- Focus on impact and results, not just duties")
                    appendLine("- Use industry-relevant keywords")
                }
                "SKILLS", "TECHNICAL SKILLS" -> {
                    appendLine("- Organize skills by category (Technical, Soft Skills, Tools, etc.)")
                    appendLine("- Prioritize skills relevant to the target role")
                    appendLine("- Use standard industry terminology")
                    appendLine("- Include proficiency levels where appropriate")
                }
                "EDUCATION" -> {
                    appendLine("- Include degree, institution, graduation date, and GPA if strong (3.5+)")
                    appendLine("- Add relevant coursework, honors, or certifications")
                    appendLine("- Include relevant academic projects or thesis topics")
                }
                else -> {
                    appendLine("- Make the content more impactful and professionally worded")
                    appendLine("- Use strong action verbs and quantifiable achievements")
                    appendLine("- Ensure ATS compatibility with proper formatting")
                }
            }
            appendLine()
            appendLine("Respond ONLY with valid JSON:")
            appendLine("""{
  "rewrittenContent": "<the improved section content>",
  "changes": ["<list of key changes made>"],
  "keywords": ["<relevant keywords added>"],
  "tips": ["<additional tips for this section>"]
}""")
        }
        
        val response = callGemini(prompt)
        return parseSectionRewriteResponse(response)
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

    private suspend fun callGemini(prompt: String): String {
        // Fallback to local API keys if backend fails
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
        
        throw IllegalStateException("AI service unavailable. Please try again later.")
    }
    
    private suspend fun callGeminiApi(prompt: String, apiKey: String): String {
        val requestBody = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            )
        )
        
        val response: GeminiResponse = httpClient.post("$geminiBaseUrl?key=$apiKey") {
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

// Note: SectionRewriteResult is imported from domain.usecase

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
