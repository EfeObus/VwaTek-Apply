package com.vwatek.apply.routes

import com.vwatek.apply.services.AIService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class AnalyzeResumeRequest(
    val resumeContent: String,
    val jobDescription: String
)

@Serializable
data class AnalyzeResumeResponse(
    val matchScore: Int,
    val missingKeywords: List<String>,
    val recommendations: List<String>
)

@Serializable
data class OptimizeResumeRequest(
    val resumeContent: String,
    val jobDescription: String
)

@Serializable
data class OptimizeResumeResponse(
    val optimizedContent: String
)

@Serializable
data class GenerateCoverLetterRequest(
    val resumeContent: String,
    val jobTitle: String,
    val companyName: String,
    val jobDescription: String,
    val tone: String = "PROFESSIONAL"
)

@Serializable
data class GenerateCoverLetterResponse(
    val content: String
)

@Serializable
data class GenerateInterviewQuestionsRequest(
    val resumeContent: String? = null,
    val jobTitle: String,
    val jobDescription: String
)

@Serializable
data class GenerateInterviewQuestionsResponse(
    val questions: List<String>
)

@Serializable
data class GetInterviewFeedbackRequest(
    val question: String,
    val answer: String,
    val jobTitle: String
)

@Serializable
data class GetInterviewFeedbackResponse(
    val feedback: String
)

@Serializable
data class StarCoachingRequest(
    val experience: String,
    val jobContext: String
)

@Serializable
data class ATSAnalysisRequest(
    val resumeContent: String,
    val jobDescription: String? = null
)

@Serializable
data class ImpactBulletsRequest(
    val experiences: List<String>,
    val jobContext: String
)

@Serializable
data class GrammarAnalysisRequest(
    val text: String
)

@Serializable
data class RewriteSectionRequest(
    val sectionType: String,
    val sectionContent: String,
    val targetRole: String? = null,
    val targetIndustry: String? = null,
    val style: String = "professional"
)

@Serializable
data class AITextResponse(
    val result: String
)

fun Route.aiRoutes(aiService: AIService) {
    authenticate("jwt") {
        route("/ai") {
        // Analyze resume against job description
        post("/analyze-resume") {
            try {
                val request = call.receive<AnalyzeResumeRequest>()
                val result = aiService.analyzeResume(request.resumeContent, request.jobDescription)
                
                call.respond(AnalyzeResumeResponse(
                    matchScore = result.matchScore,
                    missingKeywords = result.missingKeywords,
                    recommendations = result.recommendations
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "AI analysis failed")))
            }
        }
        
        // Optimize resume for job description
        post("/optimize-resume") {
            try {
                val request = call.receive<OptimizeResumeRequest>()
                val optimizedContent = aiService.optimizeResume(request.resumeContent, request.jobDescription)
                
                call.respond(OptimizeResumeResponse(optimizedContent = optimizedContent))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "AI optimization failed")))
            }
        }
        
        // Generate cover letter
        post("/generate-cover-letter") {
            try {
                val request = call.receive<GenerateCoverLetterRequest>()
                val content = aiService.generateCoverLetter(
                    resumeContent = request.resumeContent,
                    jobTitle = request.jobTitle,
                    companyName = request.companyName,
                    jobDescription = request.jobDescription,
                    tone = request.tone
                )
                
                call.respond(GenerateCoverLetterResponse(content = content))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Cover letter generation failed")))
            }
        }
        
        // Generate interview questions
        post("/generate-interview-questions") {
            try {
                val request = call.receive<GenerateInterviewQuestionsRequest>()
                val questions = aiService.generateInterviewQuestions(
                    resumeContent = request.resumeContent,
                    jobTitle = request.jobTitle,
                    jobDescription = request.jobDescription
                )
                
                call.respond(GenerateInterviewQuestionsResponse(questions = questions))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Interview question generation failed")))
            }
        }
        
        // Get interview feedback
        post("/interview-feedback") {
            try {
                val request = call.receive<GetInterviewFeedbackRequest>()
                val feedback = aiService.getInterviewFeedback(
                    question = request.question,
                    answer = request.answer,
                    jobTitle = request.jobTitle
                )
                
                call.respond(GetInterviewFeedbackResponse(feedback = feedback))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Feedback generation failed")))
            }
        }
        
        // STAR method coaching
        post("/star-coaching") {
            try {
                val request = call.receive<StarCoachingRequest>()
                val result = aiService.getStarCoaching(request.experience, request.jobContext)
                call.respond(AITextResponse(result = result))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "STAR coaching failed")))
            }
        }
        
        // ATS analysis
        post("/ats-analysis") {
            try {
                val request = call.receive<ATSAnalysisRequest>()
                val result = aiService.performATSAnalysis(request.resumeContent, request.jobDescription)
                call.respond(AITextResponse(result = result))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "ATS analysis failed")))
            }
        }
        
        // Impact bullets
        post("/impact-bullets") {
            try {
                val request = call.receive<ImpactBulletsRequest>()
                val result = aiService.generateImpactBullets(request.experiences, request.jobContext)
                call.respond(AITextResponse(result = result))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Impact bullets generation failed")))
            }
        }
        
        // Grammar and tone analysis
        post("/grammar-analysis") {
            try {
                val request = call.receive<GrammarAnalysisRequest>()
                val result = aiService.analyzeGrammarAndTone(request.text)
                call.respond(AITextResponse(result = result))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Grammar analysis failed")))
            }
        }
        
        // Rewrite resume section
        post("/rewrite-section") {
            try {
                val request = call.receive<RewriteSectionRequest>()
                val result = aiService.rewriteResumeSection(
                    sectionType = request.sectionType,
                    sectionContent = request.sectionContent,
                    targetRole = request.targetRole,
                    targetIndustry = request.targetIndustry,
                    style = request.style
                )
                call.respond(AITextResponse(result = result))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Section rewrite failed")))
            }
        }
    }
    }
}
