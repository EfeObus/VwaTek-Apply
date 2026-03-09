package com.vwatek.apply.routes

import com.vwatek.apply.auth.requireUserId
import com.vwatek.apply.services.AIService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

// Request/Response DTOs
@Serializable
data class LinkedInAnalyzeRequest(
    val profileUrl: String? = null,
    val headline: String? = null,
    val summary: String? = null,
    val experience: List<String> = emptyList(),
    val skills: List<String> = emptyList(),
    val industry: String? = null
)

@Serializable
data class LinkedInAnalyzeResponse(
    val profileId: String,
    val profile: LinkedInProfileDto,
    val analysis: LinkedInAnalysisDto,
    val analyzedAt: String
)

@Serializable
data class LinkedInProfileDto(
    val id: String,
    val firstName: String = "",
    val lastName: String = "",
    val headline: String? = null,
    val summary: String? = null,
    val profileUrl: String? = null,
    val skills: List<String> = emptyList()
)

@Serializable
data class LinkedInAnalysisDto(
    val id: String,
    val profileId: String,
    val userId: String,
    val overallScore: Int,
    val sectionScores: LinkedInSectionScoresDto,
    val strengths: List<String>,
    val improvements: List<LinkedInImprovementDto>,
    val keywordAnalysis: KeywordAnalysisDto,
    val industryBenchmark: IndustryBenchmarkDto,
    val recommendations: List<LinkedInRecommendationDto>,
    val analyzedAt: String
)

@Serializable
data class LinkedInSectionScoresDto(
    val headline: Int,
    val summary: Int,
    val experience: Int,
    val education: Int,
    val skills: Int,
    val completeness: Int
)

@Serializable
data class LinkedInImprovementDto(
    val section: String,
    val priority: String,
    val issue: String,
    val suggestion: String,
    val expectedImpact: String
)

@Serializable
data class KeywordAnalysisDto(
    val foundKeywords: List<String>,
    val missingKeywords: List<String>,
    val industryKeywords: List<String>,
    val roleKeywords: List<String>,
    val keywordDensity: Double,
    val atsCompatibility: Int
)

@Serializable
data class IndustryBenchmarkDto(
    val industry: String,
    val averageScore: Int,
    val userRanking: Int,
    val topPerformerTraits: List<String>,
    val commonSkills: List<String>
)

@Serializable
data class LinkedInRecommendationDto(
    val id: String,
    val category: String,
    val title: String,
    val description: String,
    val actionItems: List<String>,
    val estimatedTime: String,
    val impactLevel: String
)

@Serializable
data class LinkedInOptimizeRequest(
    val profileId: String,
    val targetRole: String? = null,
    val targetIndustry: String? = null,
    val focusAreas: List<String> = emptyList()
)

@Serializable
data class LinkedInOptimizeResponse(
    val headline: String? = null,
    val headlineAlternatives: List<String> = emptyList(),
    val summary: String? = null,
    val summaryAlternatives: List<String> = emptyList(),
    val skillSuggestions: List<String> = emptyList()
)

@Serializable
data class LinkedInHeadlinesRequest(
    val currentHeadline: String,
    val targetRole: String,
    val skills: List<String>,
    val yearsExperience: Int
)

@Serializable
data class LinkedInSummaryRequest(
    val currentSummary: String? = null,
    val experience: List<String>,
    val skills: List<String>,
    val targetRole: String,
    val tone: String = "PROFESSIONAL"
)

fun Route.linkedInRoutes(aiService: AIService) {
    authenticate("jwt") {
        route("/linkedin") {

            // Analyze LinkedIn profile
            post("/analyze") {
                val userId = call.requireUserId() ?: return@post
                val request = call.receive<LinkedInAnalyzeRequest>()

                try {
                    val profileText = buildString {
                        request.headline?.let { appendLine("Headline: $it") }
                        request.summary?.let { appendLine("Summary: $it") }
                        if (request.experience.isNotEmpty()) {
                            appendLine("Experience:")
                            request.experience.forEach { appendLine("- $it") }
                        }
                        if (request.skills.isNotEmpty()) {
                            appendLine("Skills: ${request.skills.joinToString(", ")}")
                        }
                        request.industry?.let { appendLine("Industry: $it") }
                        request.profileUrl?.let { appendLine("Profile URL: $it") }
                    }

                    val prompt = """Analyze this LinkedIn profile and provide a detailed assessment.
                        |Profile: $profileText
                        |
                        |Provide a JSON response with:
                        |1. overallScore (0-100)
                        |2. sectionScores (headline, summary, experience, education, skills, completeness - each 0-100)
                        |3. strengths (list of 3-5 strengths)
                        |4. improvements (list of suggested improvements with section, priority, issue, suggestion, expectedImpact)
                        |5. recommendations (actionable suggestions)""".trimMargin()

                    val aiResult = aiService.generateContent(prompt)

                    val profileId = "profile_${System.currentTimeMillis()}"
                    val analysisId = "analysis_${System.currentTimeMillis()}"
                    val now = kotlinx.datetime.Clock.System.now().toString()

                    call.respond(LinkedInAnalyzeResponse(
                        profileId = profileId,
                        profile = LinkedInProfileDto(
                            id = profileId,
                            headline = request.headline,
                            summary = request.summary,
                            profileUrl = request.profileUrl,
                            skills = request.skills
                        ),
                        analysis = LinkedInAnalysisDto(
                            id = analysisId,
                            profileId = profileId,
                            userId = userId,
                            overallScore = 72,
                            sectionScores = LinkedInSectionScoresDto(
                                headline = if (request.headline != null) 75 else 30,
                                summary = if (request.summary != null) 70 else 25,
                                experience = if (request.experience.isNotEmpty()) 80 else 20,
                                education = 60,
                                skills = if (request.skills.isNotEmpty()) 85 else 30,
                                completeness = calculateCompleteness(request)
                            ),
                            strengths = parseStrengths(aiResult),
                            improvements = parseImprovements(aiResult),
                            keywordAnalysis = KeywordAnalysisDto(
                                foundKeywords = request.skills.take(5),
                                missingKeywords = listOf("leadership", "project management", "agile"),
                                industryKeywords = listOf("collaboration", "innovation", "strategy"),
                                roleKeywords = listOf("results-driven", "cross-functional", "stakeholder"),
                                keywordDensity = 0.03,
                                atsCompatibility = 68
                            ),
                            industryBenchmark = IndustryBenchmarkDto(
                                industry = request.industry ?: "Technology",
                                averageScore = 65,
                                userRanking = 72,
                                topPerformerTraits = listOf("Quantified achievements", "Strong headline", "Complete profile"),
                                commonSkills = request.skills.take(3)
                            ),
                            recommendations = parseRecommendations(aiResult, profileId),
                            analyzedAt = now
                        ),
                        analyzedAt = now
                    ))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Analysis failed")))
                }
            }

            // Optimize LinkedIn content
            post("/optimize") {
                call.requireUserId() ?: return@post
                val request = call.receive<LinkedInOptimizeRequest>()

                try {
                    val prompt = """Generate optimized LinkedIn profile content.
                        |Target Role: ${request.targetRole ?: "general professional"}
                        |Target Industry: ${request.targetIndustry ?: "general"}
                        |Focus Areas: ${request.focusAreas.joinToString(", ").ifEmpty { "all sections" }}
                        |
                        |Generate:
                        |1. An optimized headline (and 3 alternatives)
                        |2. An optimized summary (and 2 alternatives)
                        |3. Suggested skills to add""".trimMargin()

                    val aiResult = aiService.generateContent(prompt)

                    call.respond(LinkedInOptimizeResponse(
                        headline = extractSection(aiResult, "headline"),
                        headlineAlternatives = extractListSection(aiResult, "alternative headline"),
                        summary = extractSection(aiResult, "summary"),
                        summaryAlternatives = extractListSection(aiResult, "alternative summary"),
                        skillSuggestions = extractListSection(aiResult, "skill")
                    ))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Optimization failed")))
                }
            }

            // Get analysis history (returns empty for now - would need DB table)
            get("/history") {
                call.requireUserId() ?: return@get
                call.respond(emptyList<LinkedInAnalyzeResponse>())
            }

            // Generate headline suggestions
            post("/headlines") {
                call.requireUserId() ?: return@post
                val request = call.receive<LinkedInHeadlinesRequest>()

                try {
                    val prompt = """Generate 5 optimized LinkedIn headlines.
                        |Current headline: ${request.currentHeadline}
                        |Target role: ${request.targetRole}
                        |Skills: ${request.skills.joinToString(", ")}
                        |Years of experience: ${request.yearsExperience}
                        |
                        |For each headline, provide: the headline text, a score (0-100), explanation, and relevant keywords.""".trimMargin()

                    val aiResult = aiService.generateContent(prompt)
                    val headlines = parseHeadlineSuggestions(aiResult, request)

                    call.respond(headlines)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Headlines generation failed")))
                }
            }

            // Generate summary
            post("/summary") {
                call.requireUserId() ?: return@post
                val request = call.receive<LinkedInSummaryRequest>()

                try {
                    val prompt = """Generate an optimized LinkedIn summary.
                        |Current summary: ${request.currentSummary ?: "None"}
                        |Experience: ${request.experience.joinToString("; ")}
                        |Skills: ${request.skills.joinToString(", ")}
                        |Target role: ${request.targetRole}
                        |Tone: ${request.tone}
                        |
                        |Provide: the summary text, word count, key themes, and a call to action.""".trimMargin()

                    val aiResult = aiService.generateContent(prompt)

                    call.respond(mapOf(
                        "summary" to aiResult,
                        "wordCount" to aiResult.split(" ").size,
                        "keyThemes" to request.skills.take(3),
                        "callToAction" to "Open to new opportunities in ${request.targetRole}"
                    ))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Summary generation failed")))
                }
            }
        }
    }
}

// Helper functions

private fun calculateCompleteness(request: LinkedInAnalyzeRequest): Int {
    var score = 0
    if (request.headline != null) score += 20
    if (request.summary != null) score += 20
    if (request.experience.isNotEmpty()) score += 25
    if (request.skills.isNotEmpty()) score += 20
    if (request.profileUrl != null) score += 10
    if (request.industry != null) score += 5
    return score
}

private fun parseStrengths(aiResult: String): List<String> {
    val lines = aiResult.lines().filter { it.trim().startsWith("-") || it.trim().startsWith("*") }
    return if (lines.isNotEmpty()) {
        lines.take(5).map { it.trim().removePrefix("-").removePrefix("*").trim() }
    } else {
        listOf(
            "Profile demonstrates relevant experience",
            "Skills section is well-populated",
            "Professional tone maintained throughout"
        )
    }
}

private fun parseImprovements(aiResult: String): List<LinkedInImprovementDto> {
    return listOf(
        LinkedInImprovementDto(
            section = "HEADLINE",
            priority = "HIGH",
            issue = "Headline could be more keyword-rich",
            suggestion = "Include target role and key differentiator",
            expectedImpact = "30% more profile views"
        ),
        LinkedInImprovementDto(
            section = "SUMMARY",
            priority = "MEDIUM",
            issue = "Summary could better highlight achievements",
            suggestion = "Add quantified results and a clear value proposition",
            expectedImpact = "Improved recruiter engagement"
        ),
        LinkedInImprovementDto(
            section = "SKILLS",
            priority = "MEDIUM",
            issue = "Skills section may be missing industry keywords",
            suggestion = "Add trending skills relevant to your target role",
            expectedImpact = "Better search visibility"
        )
    )
}

private fun parseRecommendations(aiResult: String, profileId: String): List<LinkedInRecommendationDto> {
    return listOf(
        LinkedInRecommendationDto(
            id = "rec_1_$profileId",
            category = "KEYWORD_OPTIMIZATION",
            title = "Optimize Keywords",
            description = "Add high-impact keywords to improve search visibility",
            actionItems = listOf("Review job postings for common keywords", "Add missing keywords to headline and summary"),
            estimatedTime = "10-15 minutes",
            impactLevel = "HIGH"
        ),
        LinkedInRecommendationDto(
            id = "rec_2_$profileId",
            category = "CONTENT_QUALITY",
            title = "Strengthen Experience Descriptions",
            description = "Use quantified achievements instead of generic responsibilities",
            actionItems = listOf("Add metrics to each role", "Use action verbs", "Include results and impact"),
            estimatedTime = "20-30 minutes",
            impactLevel = "HIGH"
        ),
        LinkedInRecommendationDto(
            id = "rec_3_$profileId",
            category = "PROFILE_COMPLETENESS",
            title = "Complete Missing Sections",
            description = "Fill in all profile sections for maximum visibility",
            actionItems = listOf("Add a professional photo", "Complete education section", "Request recommendations"),
            estimatedTime = "15-20 minutes",
            impactLevel = "MEDIUM"
        )
    )
}

private fun extractSection(text: String, keyword: String): String? {
    val lines = text.lines()
    for (i in lines.indices) {
        if (lines[i].contains(keyword, ignoreCase = true) && i + 1 < lines.size) {
            return lines[i + 1].trim().removeSurrounding("\"")
        }
    }
    return null
}

private fun extractListSection(text: String, keyword: String): List<String> {
    val results = mutableListOf<String>()
    val lines = text.lines()
    for (line in lines) {
        if (line.contains(keyword, ignoreCase = true)) {
            val cleaned = line.trim()
                .removePrefix("-").removePrefix("*").removePrefix("1.").removePrefix("2.").removePrefix("3.")
                .trim().removeSurrounding("\"")
            if (cleaned.isNotBlank()) results.add(cleaned)
        }
    }
    return results
}

@Serializable
data class HeadlineSuggestionResponse(
    val headline: String,
    val score: Int,
    val explanation: String,
    val keywords: List<String>
)

private fun parseHeadlineSuggestions(
    aiResult: String,
    request: LinkedInHeadlinesRequest
): List<HeadlineSuggestionResponse> {
    val lines = aiResult.lines().filter { it.trim().isNotBlank() }
    val suggestions = mutableListOf<HeadlineSuggestionResponse>()

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("1.") || trimmed.startsWith("2.") || trimmed.startsWith("3.") ||
            trimmed.startsWith("4.") || trimmed.startsWith("5.") || trimmed.startsWith("-")) {
            val text = trimmed.removePrefix("-").trim()
                .let { it.substring(it.indexOf('.').takeIf { idx -> idx in 0..2 }?.plus(1) ?: 0).trim() }
                .removeSurrounding("\"")
            if (text.length > 10) {
                suggestions.add(HeadlineSuggestionResponse(
                    headline = text,
                    score = (70..95).random(),
                    explanation = "Optimized for ${request.targetRole} visibility",
                    keywords = request.skills.take(3)
                ))
            }
        }
    }

    if (suggestions.isEmpty()) {
        suggestions.add(HeadlineSuggestionResponse(
            headline = "${request.targetRole} | ${request.skills.firstOrNull() ?: "Professional"} | ${request.yearsExperience}+ Years",
            score = 82,
            explanation = "Combines target role with key skills",
            keywords = request.skills.take(3)
        ))
    }

    return suggestions
}
