package com.vwatek.apply.data.api

import com.vwatek.apply.domain.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

/**
 * Phase 2: Job Tracker API Client
 * Handles all job application tracker communication with the backend
 */
class JobTrackerApiClient(
    private val httpClient: HttpClient
) {
    private val baseUrl = "${ApiConfig.apiV1Url}/jobs"
    
    // ===== Job Applications =====
    
    suspend fun getApplications(
        status: String? = null,
        source: String? = null,
        province: String? = null,
        search: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Result<JobApplicationsListDto> {
        return try {
            val response = httpClient.get(baseUrl) {
                status?.let { parameter("status", it) }
                source?.let { parameter("source", it) }
                province?.let { parameter("province", it) }
                search?.let { parameter("search", it) }
                parameter("limit", limit)
                parameter("offset", offset)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getApplication(id: String): Result<JobApplicationDetailDto> {
        return try {
            val response = httpClient.get("$baseUrl/$id")
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createApplication(request: CreateJobApplicationDto): Result<CreateJobApplicationResponseDto> {
        return try {
            val response = httpClient.post(baseUrl) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun quickSaveJob(request: QuickSaveJobDto): Result<QuickSaveResponseDto> {
        return try {
            val response = httpClient.post("$baseUrl/quick") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateApplication(id: String, request: UpdateJobApplicationDto): Result<Unit> {
        return try {
            httpClient.put("$baseUrl/$id") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateStatus(id: String, status: String, notes: String? = null): Result<Unit> {
        return try {
            httpClient.patch("$baseUrl/$id/status") {
                contentType(ContentType.Application.Json)
                setBody(UpdateStatusDto(status, notes))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteApplication(id: String): Result<Unit> {
        return try {
            httpClient.delete("$baseUrl/$id")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ===== Notes =====
    
    suspend fun addNote(applicationId: String, content: String, noteType: String = "GENERAL"): Result<String> {
        return try {
            val response = httpClient.post("$baseUrl/$applicationId/notes") {
                contentType(ContentType.Application.Json)
                setBody(CreateNoteDto(content, noteType))
            }
            val result: Map<String, String> = response.body()
            Result.success(result["id"] ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ===== Reminders =====
    
    suspend fun addReminder(
        applicationId: String,
        reminderType: String,
        title: String,
        message: String?,
        reminderAt: String
    ): Result<String> {
        return try {
            val response = httpClient.post("$baseUrl/$applicationId/reminders") {
                contentType(ContentType.Application.Json)
                setBody(CreateReminderDto(reminderType, title, message, reminderAt))
            }
            val result: Map<String, String> = response.body()
            Result.success(result["id"] ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun completeReminder(applicationId: String, reminderId: String): Result<Unit> {
        return try {
            httpClient.patch("$baseUrl/$applicationId/reminders/$reminderId/complete")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ===== Interviews =====
    
    suspend fun addInterview(
        applicationId: String,
        interviewType: String,
        scheduledAt: String,
        durationMinutes: Int? = null,
        location: String? = null,
        interviewerName: String? = null,
        interviewerTitle: String? = null,
        notes: String? = null
    ): Result<String> {
        return try {
            val response = httpClient.post("$baseUrl/$applicationId/interviews") {
                contentType(ContentType.Application.Json)
                setBody(CreateInterviewDto(
                    interviewType, scheduledAt, durationMinutes, location,
                    interviewerName, interviewerTitle, notes
                ))
            }
            val result: Map<String, String> = response.body()
            Result.success(result["id"] ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ===== Statistics =====
    
    suspend fun getStats(): Result<TrackerStatsDto> {
        return try {
            val response = httpClient.get("$baseUrl/stats")
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ===== DTOs =====

@Serializable
data class JobApplicationsListDto(
    val applications: List<JobApplicationDto>,
    val total: Int,
    val limit: Int,
    val offset: Int
)

@Serializable
data class JobApplicationDto(
    val id: String,
    val jobTitle: String,
    val companyName: String,
    val companyLogo: String? = null,
    val jobUrl: String? = null,
    val jobBoardSource: String? = null,
    val city: String? = null,
    val province: String? = null,
    val isRemote: Boolean = false,
    val isHybrid: Boolean = false,
    val salaryMin: Int? = null,
    val salaryMax: Int? = null,
    val salaryCurrency: String = "CAD",
    val salaryPeriod: String? = null,
    val status: String,
    val appliedAt: String? = null,
    val nocCode: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class JobApplicationDetailDto(
    val application: JobApplicationDto,
    val notes: List<NoteDto>,
    val reminders: List<ReminderDto>,
    val interviews: List<InterviewDto>,
    val statusHistory: List<StatusChangeDto>
)

@Serializable
data class NoteDto(
    val id: String,
    val content: String,
    val noteType: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ReminderDto(
    val id: String,
    val reminderType: String,
    val title: String,
    val message: String? = null,
    val reminderAt: String,
    val isCompleted: Boolean,
    val completedAt: String? = null
)

@Serializable
data class InterviewDto(
    val id: String,
    val interviewType: String,
    val scheduledAt: String,
    val durationMinutes: Int? = null,
    val location: String? = null,
    val interviewerName: String? = null,
    val interviewerTitle: String? = null,
    val status: String
)

@Serializable
data class StatusChangeDto(
    val id: String,
    val fromStatus: String? = null,
    val toStatus: String,
    val notes: String? = null,
    val changedAt: String
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
    val country: String? = null,
    val isRemote: Boolean? = null,
    val isHybrid: Boolean? = null,
    val salaryMin: Int? = null,
    val salaryMax: Int? = null,
    val salaryCurrency: String? = null,
    val salaryPeriod: String? = null,
    val status: String? = null,
    val appliedAt: String? = null,
    val nocCode: String? = null,
    val requiresWorkPermit: Boolean? = null,
    val isLmiaRequired: Boolean? = null,
    val contactName: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null
)

@Serializable
data class CreateJobApplicationResponseDto(
    val id: String,
    val message: String
)

@Serializable
data class QuickSaveJobDto(
    val jobTitle: String,
    val companyName: String,
    val jobUrl: String? = null,
    val jobBoardSource: String? = null,
    val externalJobId: String? = null
)

@Serializable
data class QuickSaveResponseDto(
    val id: String,
    val message: String,
    val alreadyExists: Boolean
)

@Serializable
data class UpdateJobApplicationDto(
    val jobTitle: String? = null,
    val companyName: String? = null,
    val companyLogo: String? = null,
    val jobUrl: String? = null,
    val jobDescription: String? = null,
    val city: String? = null,
    val province: String? = null,
    val isRemote: Boolean? = null,
    val isHybrid: Boolean? = null,
    val salaryMin: Int? = null,
    val salaryMax: Int? = null,
    val salaryPeriod: String? = null,
    val nocCode: String? = null,
    val contactName: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null
)

@Serializable
data class UpdateStatusDto(
    val status: String,
    val notes: String? = null
)

@Serializable
data class CreateNoteDto(
    val content: String,
    val noteType: String? = null
)

@Serializable
data class CreateReminderDto(
    val reminderType: String,
    val title: String,
    val message: String? = null,
    val reminderAt: String
)

@Serializable
data class CreateInterviewDto(
    val interviewType: String,
    val scheduledAt: String,
    val durationMinutes: Int? = null,
    val location: String? = null,
    val interviewerName: String? = null,
    val interviewerTitle: String? = null,
    val notes: String? = null
)

@Serializable
data class TrackerStatsDto(
    val totalApplications: Int,
    val savedCount: Int,
    val appliedCount: Int,
    val interviewCount: Int,
    val offerCount: Int,
    val rejectedCount: Int,
    val interviewRate: Float,
    val offerRate: Float
)
