package com.vwatek.apply.routes

import com.vwatek.apply.db.tables.InterviewSessionsTable
import com.vwatek.apply.db.tables.InterviewQuestionsTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

@Serializable
data class InterviewSessionRequest(
    val resumeId: String? = null,
    val jobTitle: String,
    val jobDescription: String
)

@Serializable
data class InterviewQuestionRequest(
    val question: String,
    val questionOrder: Int
)

@Serializable
data class AnswerRequest(
    val answer: String,
    val feedback: String? = null
)

@Serializable
data class InterviewStatusUpdateRequest(
    val status: String,
    val completedAt: Long? = null
)

@Serializable
data class InterviewQuestionResponse(
    val id: String,
    val sessionId: String,
    val question: String,
    val userAnswer: String?,
    val aiFeedback: String?,
    val questionOrder: Int,
    val createdAt: String
)

@Serializable
data class InterviewSessionResponse(
    val id: String,
    val resumeId: String?,
    val jobTitle: String,
    val jobDescription: String,
    val status: String,
    val questions: List<InterviewQuestionResponse>,
    val createdAt: String,
    val completedAt: String?
)

fun Route.interviewRoutes() {
    route("/interviews") {
        // Get all interview sessions
        get {
            val userId = call.request.headers["X-User-Id"]
            
            val sessions = transaction {
                val query = if (userId != null) {
                    InterviewSessionsTable.select { InterviewSessionsTable.userId eq userId }
                } else {
                    InterviewSessionsTable.selectAll()
                }
                query.orderBy(InterviewSessionsTable.createdAt, SortOrder.DESC)
                    .map { row ->
                        val sessionId = row[InterviewSessionsTable.id]
                        val questions = InterviewQuestionsTable
                            .select { InterviewQuestionsTable.sessionId eq sessionId }
                            .orderBy(InterviewQuestionsTable.questionOrder, SortOrder.ASC)
                            .map { q ->
                                InterviewQuestionResponse(
                                    id = q[InterviewQuestionsTable.id],
                                    sessionId = q[InterviewQuestionsTable.sessionId],
                                    question = q[InterviewQuestionsTable.question],
                                    userAnswer = q[InterviewQuestionsTable.userAnswer],
                                    aiFeedback = q[InterviewQuestionsTable.aiFeedback],
                                    questionOrder = q[InterviewQuestionsTable.questionOrder],
                                    createdAt = q[InterviewQuestionsTable.createdAt].toString()
                                )
                            }
                        
                        InterviewSessionResponse(
                            id = sessionId,
                            resumeId = row[InterviewSessionsTable.resumeId],
                            jobTitle = row[InterviewSessionsTable.jobTitle],
                            jobDescription = row[InterviewSessionsTable.jobDescription],
                            status = row[InterviewSessionsTable.status],
                            questions = questions,
                            createdAt = row[InterviewSessionsTable.createdAt].toString(),
                            completedAt = row[InterviewSessionsTable.completedAt]?.toString()
                        )
                    }
            }
            
            call.respond(sessions)
        }
        
        // Get single interview session
        get("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing session ID")
            
            val session = transaction {
                InterviewSessionsTable.select { InterviewSessionsTable.id eq id }.firstOrNull()
            }
            
            if (session == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Interview session not found"))
                return@get
            }
            
            val questions = transaction {
                InterviewQuestionsTable
                    .select { InterviewQuestionsTable.sessionId eq id }
                    .orderBy(InterviewQuestionsTable.questionOrder, SortOrder.ASC)
                    .map { q ->
                        InterviewQuestionResponse(
                            id = q[InterviewQuestionsTable.id],
                            sessionId = q[InterviewQuestionsTable.sessionId],
                            question = q[InterviewQuestionsTable.question],
                            userAnswer = q[InterviewQuestionsTable.userAnswer],
                            aiFeedback = q[InterviewQuestionsTable.aiFeedback],
                            questionOrder = q[InterviewQuestionsTable.questionOrder],
                            createdAt = q[InterviewQuestionsTable.createdAt].toString()
                        )
                    }
            }
            
            call.respond(InterviewSessionResponse(
                id = session[InterviewSessionsTable.id],
                resumeId = session[InterviewSessionsTable.resumeId],
                jobTitle = session[InterviewSessionsTable.jobTitle],
                jobDescription = session[InterviewSessionsTable.jobDescription],
                status = session[InterviewSessionsTable.status],
                questions = questions,
                createdAt = session[InterviewSessionsTable.createdAt].toString(),
                completedAt = session[InterviewSessionsTable.completedAt]?.toString()
            ))
        }
        
        // Create interview session
        post {
            val request = call.receive<InterviewSessionRequest>()
            val userId = call.request.headers["X-User-Id"]
            
            val sessionId = UUID.randomUUID().toString()
            val now = Clock.System.now()
            
            transaction {
                InterviewSessionsTable.insert {
                    it[id] = sessionId
                    it[InterviewSessionsTable.userId] = userId
                    it[resumeId] = request.resumeId
                    it[jobTitle] = request.jobTitle
                    it[jobDescription] = request.jobDescription
                    it[status] = "IN_PROGRESS"
                    it[createdAt] = now
                }
            }
            
            call.respond(HttpStatusCode.Created, InterviewSessionResponse(
                id = sessionId,
                resumeId = request.resumeId,
                jobTitle = request.jobTitle,
                jobDescription = request.jobDescription,
                status = "IN_PROGRESS",
                questions = emptyList(),
                createdAt = now.toString(),
                completedAt = null
            ))
        }
        
        // Add question to session
        post("/{id}/questions") {
            val sessionId = call.parameters["id"] ?: throw IllegalArgumentException("Missing session ID")
            val request = call.receive<InterviewQuestionRequest>()
            
            val questionId = UUID.randomUUID().toString()
            val now = Clock.System.now()
            
            transaction {
                InterviewQuestionsTable.insert {
                    it[id] = questionId
                    it[InterviewQuestionsTable.sessionId] = sessionId
                    it[question] = request.question
                    it[questionOrder] = request.questionOrder
                    it[createdAt] = now
                }
            }
            
            call.respond(HttpStatusCode.Created, InterviewQuestionResponse(
                id = questionId,
                sessionId = sessionId,
                question = request.question,
                userAnswer = null,
                aiFeedback = null,
                questionOrder = request.questionOrder,
                createdAt = now.toString()
            ))
        }
        
        // Submit answer for a question (simpler route - frontend compatible)
        put("/questions/{questionId}/answer") {
            val questionId = call.parameters["questionId"] ?: throw IllegalArgumentException("Missing question ID")
            val request = call.receive<AnswerRequest>()
            
            val updated = transaction {
                InterviewQuestionsTable.update({ InterviewQuestionsTable.id eq questionId }) {
                    it[userAnswer] = request.answer
                    it[aiFeedback] = request.feedback
                }
            }
            
            if (updated == 0) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Question not found"))
                return@put
            }
            
            call.respond(mapOf("success" to true))
        }
        
        // Submit answer for a question
        put("/{sessionId}/questions/{questionId}/answer") {
            val questionId = call.parameters["questionId"] ?: throw IllegalArgumentException("Missing question ID")
            val request = call.receive<AnswerRequest>()
            
            val updated = transaction {
                InterviewQuestionsTable.update({ InterviewQuestionsTable.id eq questionId }) {
                    it[userAnswer] = request.answer
                    it[aiFeedback] = request.feedback
                }
            }
            
            if (updated == 0) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Question not found"))
                return@put
            }
            
            call.respond(mapOf("success" to true))
        }
        
        // Update interview session status (frontend compatible)
        put("/{id}/status") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing session ID")
            val request = call.receive<InterviewStatusUpdateRequest>()
            val now = Clock.System.now()
            
            val completedAt = if (request.status == "COMPLETED" && request.completedAt != null) {
                kotlinx.datetime.Instant.fromEpochMilliseconds(request.completedAt)
            } else if (request.status == "COMPLETED") {
                now
            } else {
                null
            }
            
            val updated = transaction {
                InterviewSessionsTable.update({ InterviewSessionsTable.id eq id }) {
                    it[status] = request.status
                    if (completedAt != null) {
                        it[InterviewSessionsTable.completedAt] = completedAt
                    }
                }
            }
            
            if (updated == 0) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Session not found"))
                return@put
            }
            
            call.respond(mapOf("success" to true, "status" to request.status))
        }
        
        // Complete interview session
        post("/{id}/complete") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing session ID")
            val now = Clock.System.now()
            
            val updated = transaction {
                InterviewSessionsTable.update({ InterviewSessionsTable.id eq id }) {
                    it[status] = "COMPLETED"
                    it[completedAt] = now
                }
            }
            
            if (updated == 0) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Session not found"))
                return@post
            }
            
            call.respond(mapOf("success" to true, "completedAt" to now.toString()))
        }
        
        // Delete interview session
        delete("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing session ID")
            
            transaction {
                // Delete questions first (foreign key constraint)
                InterviewQuestionsTable.deleteWhere { InterviewQuestionsTable.sessionId eq id }
                InterviewSessionsTable.deleteWhere { InterviewSessionsTable.id eq id }
            }
            
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
