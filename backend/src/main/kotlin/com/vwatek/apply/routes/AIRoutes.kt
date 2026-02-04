package com.vwatek.apply.routes

import com.vwatek.apply.services.AIService
import io.ktor.http.*
import io.ktor.server.application.*
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

fun Route.aiRoutes(aiService: AIService) {
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
    }
}
