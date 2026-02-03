package com.vwatek.apply.data.repository

import com.vwatek.apply.domain.model.InterviewSession
import com.vwatek.apply.domain.model.InterviewQuestion
import com.vwatek.apply.domain.model.InterviewStatus
import com.vwatek.apply.domain.repository.InterviewRepository
import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.w3c.fetch.RequestInit
import kotlinx.coroutines.await
import kotlin.js.json as jsJson

/**
 * API-based Interview Repository that communicates with the backend Cloud SQL database
 */
class ApiInterviewRepository : InterviewRepository {
    
    private val _sessions = MutableStateFlow<List<InterviewSession>>(emptyList())
    
    override fun getAllSessions(): Flow<List<InterviewSession>> {
        refreshSessions()
        return _sessions.asStateFlow()
    }
    
    private fun refreshSessions() {
        launchAsync {
            try {
                val userId = getCurrentUserId()
                val response = window.fetch(
                    "${getApiBaseUrl()}/api/v1/interviews",
                    RequestInit(
                        method = "GET",
                        headers = jsJson(
                            "Content-Type" to "application/json",
                            "X-User-Id" to (userId ?: "")
                        )
                    )
                ).await()
                
                if (response.ok) {
                    val responseText = response.text().await()
                    val apiSessions = apiJson.decodeFromString<List<InterviewSessionApiResponse>>(responseText)
                    _sessions.value = apiSessions.map { it.toInterviewSession() }
                    console.log("Loaded ${apiSessions.size} interview sessions from API")
                } else {
                    console.error("Failed to load interview sessions: ${response.status}")
                }
            } catch (e: Exception) {
                console.error("Error loading interview sessions: ${e.message}")
            }
        }
    }
    
    override suspend fun getSessionById(id: String): InterviewSession? {
        return try {
            val response = window.fetch(
                "${getApiBaseUrl()}/api/v1/interviews/$id",
                RequestInit(
                    method = "GET",
                    headers = jsJson("Content-Type" to "application/json")
                )
            ).await()
            
            if (response.ok) {
                val responseText = response.text().await()
                val apiSession = apiJson.decodeFromString<InterviewSessionApiResponse>(responseText)
                apiSession.toInterviewSession()
            } else {
                null
            }
        } catch (e: Exception) {
            console.error("Error getting interview session: ${e.message}")
            null
        }
    }
    
    override suspend fun insertSession(session: InterviewSession) {
        try {
            val userId = getCurrentUserId()
            val requestBody = apiJson.encodeToString(InterviewSessionApiRequest(
                resumeId = session.resumeId,
                jobTitle = session.jobTitle,
                jobDescription = session.jobDescription
            ))
            
            val response = window.fetch(
                "${getApiBaseUrl()}/api/v1/interviews",
                RequestInit(
                    method = "POST",
                    headers = jsJson(
                        "Content-Type" to "application/json",
                        "X-User-Id" to (userId ?: "")
                    ),
                    body = requestBody
                )
            ).await()
            
            if (response.ok) {
                val responseText = response.text().await()
                val newSession = apiJson.decodeFromString<InterviewSessionApiResponse>(responseText).toInterviewSession()
                _sessions.value = _sessions.value + newSession
                console.log("Interview session created: ${newSession.id}")
            } else {
                console.error("Failed to create interview session: ${response.status}")
            }
        } catch (e: Exception) {
            console.error("Error inserting interview session: ${e.message}")
        }
    }
    
    override suspend fun updateSessionStatus(id: String, status: String, completedAt: Long?) {
        try {
            val requestBody = apiJson.encodeToString(UpdateStatusApiRequest(
                status = status,
                completedAt = completedAt
            ))
            
            val response = window.fetch(
                "${getApiBaseUrl()}/api/v1/interviews/$id/status",
                RequestInit(
                    method = "PUT",
                    headers = jsJson("Content-Type" to "application/json"),
                    body = requestBody
                )
            ).await()
            
            if (response.ok) {
                val completedInstant = completedAt?.let { Instant.fromEpochMilliseconds(it) }
                _sessions.value = _sessions.value.map { 
                    if (it.id == id) it.copy(
                        status = try { InterviewStatus.valueOf(status) } catch (e: Exception) { it.status },
                        completedAt = completedInstant
                    ) else it 
                }
                console.log("Interview session status updated: $id -> $status")
            } else {
                console.error("Failed to update session status: ${response.status}")
            }
        } catch (e: Exception) {
            console.error("Error updating session status: ${e.message}")
        }
    }
    
    override suspend fun deleteSession(id: String) {
        try {
            val response = window.fetch(
                "${getApiBaseUrl()}/api/v1/interviews/$id",
                RequestInit(
                    method = "DELETE",
                    headers = jsJson("Content-Type" to "application/json")
                )
            ).await()
            
            if (response.ok || response.status.toInt() == 204) {
                _sessions.value = _sessions.value.filter { it.id != id }
                console.log("Interview session deleted: $id")
            } else {
                console.error("Failed to delete interview session: ${response.status}")
            }
        } catch (e: Exception) {
            console.error("Error deleting interview session: ${e.message}")
        }
    }
    
    override fun getQuestionsBySessionId(sessionId: String): Flow<List<InterviewQuestion>> {
        val session = _sessions.value.find { it.id == sessionId }
        return MutableStateFlow(session?.questions ?: emptyList()).asStateFlow()
    }
    
    override suspend fun insertQuestion(question: InterviewQuestion) {
        try {
            val requestBody = apiJson.encodeToString(InterviewQuestionApiRequest(
                question = question.question,
                questionOrder = question.questionOrder
            ))
            
            val response = window.fetch(
                "${getApiBaseUrl()}/api/v1/interviews/${question.sessionId}/questions",
                RequestInit(
                    method = "POST",
                    headers = jsJson("Content-Type" to "application/json"),
                    body = requestBody
                )
            ).await()
            
            if (response.ok) {
                refreshSessions()
                console.log("Question added to session: ${question.sessionId}")
            } else {
                console.error("Failed to add question: ${response.status}")
            }
        } catch (e: Exception) {
            console.error("Error inserting question: ${e.message}")
        }
    }
    
    override suspend fun updateQuestion(id: String, userAnswer: String?, aiFeedback: String?) {
        try {
            val requestBody = apiJson.encodeToString(AnswerApiRequest(
                answer = userAnswer ?: "",
                feedback = aiFeedback
            ))
            
            val response = window.fetch(
                "${getApiBaseUrl()}/api/v1/interviews/questions/$id/answer",
                RequestInit(
                    method = "PUT",
                    headers = jsJson("Content-Type" to "application/json"),
                    body = requestBody
                )
            ).await()
            
            if (response.ok) {
                _sessions.value = _sessions.value.map { session ->
                    session.copy(questions = session.questions.map { q ->
                        if (q.id == id) q.copy(userAnswer = userAnswer, aiFeedback = aiFeedback) else q
                    })
                }
                console.log("Question updated: $id")
            } else {
                console.error("Failed to update question: ${response.status}")
            }
        } catch (e: Exception) {
            console.error("Error updating question: ${e.message}")
        }
    }
}

// API DTOs
@Serializable
private data class InterviewSessionApiRequest(
    val resumeId: String? = null,
    val jobTitle: String,
    val jobDescription: String
)

@Serializable
private data class InterviewQuestionApiRequest(
    val question: String,
    val questionOrder: Int
)

@Serializable
private data class AnswerApiRequest(
    val answer: String,
    val feedback: String? = null
)

@Serializable
private data class UpdateStatusApiRequest(
    val status: String,
    val completedAt: Long? = null
)

@Serializable
private data class InterviewQuestionApiResponse(
    val id: String,
    val sessionId: String,
    val question: String,
    val userAnswer: String? = null,
    val aiFeedback: String? = null,
    val questionOrder: Int,
    val createdAt: String
) {
    fun toInterviewQuestion(): InterviewQuestion = InterviewQuestion(
        id = id,
        sessionId = sessionId,
        question = question,
        userAnswer = userAnswer,
        aiFeedback = aiFeedback,
        questionOrder = questionOrder,
        createdAt = try { Instant.parse(createdAt) } catch (e: Exception) { Clock.System.now() }
    )
}

@Serializable
private data class InterviewSessionApiResponse(
    val id: String,
    val resumeId: String? = null,
    val jobTitle: String,
    val jobDescription: String,
    val status: String,
    val questions: List<InterviewQuestionApiResponse> = emptyList(),
    val createdAt: String,
    val completedAt: String? = null
) {
    fun toInterviewSession(): InterviewSession = InterviewSession(
        id = id,
        resumeId = resumeId,
        jobTitle = jobTitle,
        jobDescription = jobDescription,
        status = try { InterviewStatus.valueOf(status) } catch (e: Exception) { InterviewStatus.IN_PROGRESS },
        questions = questions.map { it.toInterviewQuestion() },
        createdAt = try { Instant.parse(createdAt) } catch (e: Exception) { Clock.System.now() },
        completedAt = completedAt?.let { try { Instant.parse(it) } catch (e: Exception) { null } }
    )
}
