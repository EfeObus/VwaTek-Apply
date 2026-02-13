package com.vwatek.apply.routes

import com.vwatek.apply.db.tables.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/**
 * Phase 2: Job Application Tracker API Routes
 * Provides CRUD operations for job applications, notes, reminders, and interviews
 */
fun Route.jobTrackerRoutes() {
    authenticate("jwt") {
        route("/tracker") {
            // Get all job applications for user
            get {
                val userId = call.principal<JWTPrincipal>()?.subject
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                
                val status = call.request.queryParameters["status"]
                val source = call.request.queryParameters["source"]
                val province = call.request.queryParameters["province"]
                val search = call.request.queryParameters["search"]
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                
                val applications = transaction {
                    JobApplicationsTable
                        .select { JobApplicationsTable.userId eq userId }
                        .apply {
                            status?.let { andWhere { JobApplicationsTable.status eq it } }
                            source?.let { andWhere { JobApplicationsTable.jobBoardSource eq it } }
                            province?.let { andWhere { JobApplicationsTable.province eq it } }
                            search?.let { query ->
                                andWhere {
                                    (JobApplicationsTable.jobTitle like "%$query%") or
                                    (JobApplicationsTable.companyName like "%$query%")
                                }
                            }
                        }
                        .orderBy(JobApplicationsTable.updatedAt, SortOrder.DESC)
                        .limit(limit, offset.toLong())
                        .map { it.toJobApplicationResponse() }
                }
                
                call.respond(JobApplicationsListResponse(
                    applications = applications,
                    total = applications.size,
                    limit = limit,
                    offset = offset
                ))
            }
            
            // Get job application by ID
            get("/{id}") {
                val userId = call.principal<JWTPrincipal>()?.subject
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val applicationId = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing application ID")
                
                val application = transaction {
                    JobApplicationsTable
                        .select { 
                            (JobApplicationsTable.id eq applicationId) and 
                            (JobApplicationsTable.userId eq userId) 
                        }
                        .firstOrNull()
                        ?.toJobApplicationResponse()
                }
                
                if (application == null) {
                    call.respond(HttpStatusCode.NotFound, "Application not found")
                } else {
                    // Get related data
                    val notes = transaction {
                        JobApplicationNotesTable
                            .select { JobApplicationNotesTable.applicationId eq applicationId }
                            .orderBy(JobApplicationNotesTable.createdAt, SortOrder.DESC)
                            .map { it.toNoteResponse() }
                    }
                    val reminders = transaction {
                        JobApplicationRemindersTable
                            .select { JobApplicationRemindersTable.applicationId eq applicationId }
                            .orderBy(JobApplicationRemindersTable.reminderAt, SortOrder.ASC)
                            .map { it.toReminderResponse() }
                    }
                    val interviews = transaction {
                        JobApplicationInterviewsTable
                            .select { JobApplicationInterviewsTable.applicationId eq applicationId }
                            .orderBy(JobApplicationInterviewsTable.scheduledAt, SortOrder.ASC)
                            .map { it.toInterviewResponse() }
                    }
                    val statusHistory = transaction {
                        JobApplicationStatusHistoryTable
                            .select { JobApplicationStatusHistoryTable.applicationId eq applicationId }
                            .orderBy(JobApplicationStatusHistoryTable.changedAt, SortOrder.DESC)
                            .map { it.toStatusChangeResponse() }
                    }
                    
                    call.respond(JobApplicationDetailResponse(
                        application = application,
                        notes = notes,
                        reminders = reminders,
                        interviews = interviews,
                        statusHistory = statusHistory
                    ))
                }
            }
            
            // Create new job application
            post {
                val userId = call.principal<JWTPrincipal>()?.subject
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val request = call.receive<CreateJobApplicationRequest>()
                
                val applicationId = UUID.randomUUID().toString()
                val now = Clock.System.now()
                
                transaction {
                    JobApplicationsTable.insert {
                        it[id] = applicationId
                        it[JobApplicationsTable.userId] = userId
                        it[resumeId] = request.resumeId
                        it[coverLetterId] = request.coverLetterId
                        it[jobTitle] = request.jobTitle
                        it[companyName] = request.companyName
                        it[companyLogo] = request.companyLogo
                        it[jobUrl] = request.jobUrl
                        it[jobDescription] = request.jobDescription
                        it[jobBoardSource] = request.jobBoardSource
                        it[externalJobId] = request.externalJobId
                        it[city] = request.city
                        it[province] = request.province
                        it[country] = request.country ?: "Canada"
                        it[isRemote] = request.isRemote ?: false
                        it[isHybrid] = request.isHybrid ?: false
                        it[salaryMin] = request.salaryMin
                        it[salaryMax] = request.salaryMax
                        it[salaryCurrency] = request.salaryCurrency ?: "CAD"
                        it[salaryPeriod] = request.salaryPeriod
                        it[status] = request.status ?: "SAVED"
                        it[appliedAt] = request.appliedAt?.let { ts -> Instant.parse(ts) }
                        it[nocCode] = request.nocCode
                        it[requiresWorkPermit] = request.requiresWorkPermit
                        it[isLmiaRequired] = request.isLmiaRequired
                        it[contactName] = request.contactName
                        it[contactEmail] = request.contactEmail
                        it[contactPhone] = request.contactPhone
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                    
                    // Record initial status
                    JobApplicationStatusHistoryTable.insert {
                        it[id] = UUID.randomUUID().toString()
                        it[JobApplicationStatusHistoryTable.applicationId] = applicationId
                        it[fromStatus] = null
                        it[toStatus] = request.status ?: "SAVED"
                        it[notes] = "Application created"
                        it[changedAt] = now
                    }
                }
                
                call.respond(HttpStatusCode.Created, CreateJobApplicationResponse(
                    id = applicationId,
                    message = "Job application created successfully"
                ))
            }
            
            // Quick save (minimal data from Chrome extension)
            post("/quick") {
                val userId = call.principal<JWTPrincipal>()?.subject
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val request = call.receive<QuickSaveJobRequest>()
                
                // Check if already saved (by external ID or URL)
                val existingId = transaction {
                    JobApplicationsTable
                        .slice(JobApplicationsTable.id)
                        .select {
                            (JobApplicationsTable.userId eq userId) and
                            ((JobApplicationsTable.externalJobId eq request.externalJobId) or
                             (JobApplicationsTable.jobUrl eq request.jobUrl))
                        }
                        .firstOrNull()
                        ?.get(JobApplicationsTable.id)
                }
                
                if (existingId != null) {
                    call.respond(HttpStatusCode.Conflict, QuickSaveJobResponse(
                        id = existingId,
                        message = "Job already saved",
                        alreadyExists = true
                    ))
                    return@post
                }
                
                val applicationId = UUID.randomUUID().toString()
                val now = Clock.System.now()
                
                transaction {
                    JobApplicationsTable.insert {
                        it[id] = applicationId
                        it[JobApplicationsTable.userId] = userId
                        it[jobTitle] = request.jobTitle
                        it[companyName] = request.companyName
                        it[jobUrl] = request.jobUrl
                        it[jobBoardSource] = request.jobBoardSource
                        it[externalJobId] = request.externalJobId
                        it[status] = "SAVED"
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                }
                
                call.respond(HttpStatusCode.Created, QuickSaveJobResponse(
                    id = applicationId,
                    message = "Job saved successfully",
                    alreadyExists = false
                ))
            }
            
            // Update job application
            put("/{id}") {
                val userId = call.principal<JWTPrincipal>()?.subject
                    ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val applicationId = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing application ID")
                val request = call.receive<UpdateJobApplicationRequest>()
                
                val updated = transaction {
                    JobApplicationsTable.update({
                        (JobApplicationsTable.id eq applicationId) and 
                        (JobApplicationsTable.userId eq userId)
                    }) {
                        request.jobTitle?.let { value -> it[jobTitle] = value }
                        request.companyName?.let { value -> it[companyName] = value }
                        request.companyLogo?.let { value -> it[companyLogo] = value }
                        request.jobUrl?.let { value -> it[jobUrl] = value }
                        request.jobDescription?.let { value -> it[jobDescription] = value }
                        request.city?.let { value -> it[city] = value }
                        request.province?.let { value -> it[province] = value }
                        request.isRemote?.let { value -> it[isRemote] = value }
                        request.isHybrid?.let { value -> it[isHybrid] = value }
                        request.salaryMin?.let { value -> it[salaryMin] = value }
                        request.salaryMax?.let { value -> it[salaryMax] = value }
                        request.salaryPeriod?.let { value -> it[salaryPeriod] = value }
                        request.nocCode?.let { value -> it[nocCode] = value }
                        request.contactName?.let { value -> it[contactName] = value }
                        request.contactEmail?.let { value -> it[contactEmail] = value }
                        request.contactPhone?.let { value -> it[contactPhone] = value }
                        it[updatedAt] = Clock.System.now()
                    }
                }
                
                if (updated > 0) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Application updated"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Application not found")
                }
            }
            
            // Update application status
            patch("/{id}/status") {
                val userId = call.principal<JWTPrincipal>()?.subject
                    ?: return@patch call.respond(HttpStatusCode.Unauthorized)
                val applicationId = call.parameters["id"]
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, "Missing application ID")
                val request = call.receive<JobTrackerStatusUpdateRequest>()
                
                val now = Clock.System.now()
                
                transaction {
                    // Get current status
                    val currentStatus = JobApplicationsTable
                        .slice(JobApplicationsTable.status)
                        .select { 
                            (JobApplicationsTable.id eq applicationId) and 
                            (JobApplicationsTable.userId eq userId) 
                        }
                        .firstOrNull()
                        ?.get(JobApplicationsTable.status)
                        ?: return@transaction
                    
                    // Update status
                    JobApplicationsTable.update({
                        (JobApplicationsTable.id eq applicationId) and 
                        (JobApplicationsTable.userId eq userId)
                    }) {
                        it[status] = request.status
                        it[updatedAt] = now
                        if (request.status == "APPLIED" && currentStatus == "SAVED") {
                            it[appliedAt] = now
                        }
                        if (request.status in listOf("INTERVIEW", "OFFER", "REJECTED")) {
                            it[responseReceivedAt] = now
                        }
                    }
                    
                    // Record status change
                    JobApplicationStatusHistoryTable.insert {
                        it[id] = UUID.randomUUID().toString()
                        it[JobApplicationStatusHistoryTable.applicationId] = applicationId
                        it[fromStatus] = currentStatus
                        it[toStatus] = request.status
                        it[notes] = request.notes
                        it[changedAt] = now
                    }
                }
                
                call.respond(HttpStatusCode.OK, mapOf("message" to "Status updated"))
            }
            
            // Delete job application
            delete("/{id}") {
                val userId = call.principal<JWTPrincipal>()?.subject
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                val applicationId = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing application ID")
                
                val deleted = transaction {
                    // Delete related records first
                    JobApplicationNotesTable.deleteWhere { JobApplicationNotesTable.applicationId eq applicationId }
                    JobApplicationRemindersTable.deleteWhere { JobApplicationRemindersTable.applicationId eq applicationId }
                    JobApplicationInterviewsTable.deleteWhere { JobApplicationInterviewsTable.applicationId eq applicationId }
                    JobApplicationStatusHistoryTable.deleteWhere { JobApplicationStatusHistoryTable.applicationId eq applicationId }
                    JobApplicationDocumentsTable.deleteWhere { JobApplicationDocumentsTable.applicationId eq applicationId }
                    
                    // Delete application
                    JobApplicationsTable.deleteWhere { 
                        (JobApplicationsTable.id eq applicationId) and 
                        (JobApplicationsTable.userId eq userId) 
                    }
                }
                
                if (deleted > 0) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Application deleted"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Application not found")
                }
            }
            
            // ===== Notes =====
            
            // Add note to application
            post("/{id}/notes") {
                val userId = call.principal<JWTPrincipal>()?.subject
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val applicationId = call.parameters["id"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing application ID")
                val request = call.receive<CreateNoteRequest>()
                
                // Verify ownership
                val exists = transaction {
                    JobApplicationsTable
                        .select { 
                            (JobApplicationsTable.id eq applicationId) and 
                            (JobApplicationsTable.userId eq userId) 
                        }
                        .count() > 0
                }
                
                if (!exists) {
                    call.respond(HttpStatusCode.NotFound, "Application not found")
                    return@post
                }
                
                val noteId = UUID.randomUUID().toString()
                val now = Clock.System.now()
                
                transaction {
                    JobApplicationNotesTable.insert {
                        it[id] = noteId
                        it[JobApplicationNotesTable.applicationId] = applicationId
                        it[content] = request.content
                        it[noteType] = request.noteType ?: "GENERAL"
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                }
                
                call.respond(HttpStatusCode.Created, mapOf("id" to noteId))
            }
            
            // ===== Reminders =====
            
            // Add reminder to application
            post("/{id}/reminders") {
                val userId = call.principal<JWTPrincipal>()?.subject
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val applicationId = call.parameters["id"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing application ID")
                val request = call.receive<CreateReminderRequest>()
                
                val reminderId = UUID.randomUUID().toString()
                val now = Clock.System.now()
                
                transaction {
                    JobApplicationRemindersTable.insert {
                        it[id] = reminderId
                        it[JobApplicationRemindersTable.applicationId] = applicationId
                        it[reminderType] = request.reminderType
                        it[title] = request.title
                        it[message] = request.message
                        it[reminderAt] = Instant.parse(request.reminderAt)
                        it[isCompleted] = false
                        it[createdAt] = now
                    }
                }
                
                call.respond(HttpStatusCode.Created, mapOf("id" to reminderId))
            }
            
            // Mark reminder as completed
            patch("/{id}/reminders/{reminderId}/complete") {
                val userId = call.principal<JWTPrincipal>()?.subject
                    ?: return@patch call.respond(HttpStatusCode.Unauthorized)
                val reminderId = call.parameters["reminderId"]
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, "Missing reminder ID")
                
                transaction {
                    JobApplicationRemindersTable.update({ JobApplicationRemindersTable.id eq reminderId }) {
                        it[isCompleted] = true
                        it[completedAt] = Clock.System.now()
                    }
                }
                
                call.respond(HttpStatusCode.OK)
            }
            
            // ===== Interviews =====
            
            // Add interview to application
            post("/{id}/interviews") {
                val userId = call.principal<JWTPrincipal>()?.subject
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val applicationId = call.parameters["id"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing application ID")
                val request = call.receive<CreateInterviewRequest>()
                
                val interviewId = UUID.randomUUID().toString()
                val now = Clock.System.now()
                
                transaction {
                    JobApplicationInterviewsTable.insert {
                        it[id] = interviewId
                        it[JobApplicationInterviewsTable.applicationId] = applicationId
                        it[interviewType] = request.interviewType
                        it[scheduledAt] = Instant.parse(request.scheduledAt)
                        it[duration] = request.durationMinutes
                        it[location] = request.location
                        it[interviewerName] = request.interviewerName
                        it[interviewerTitle] = request.interviewerTitle
                        it[notes] = request.notes
                        it[status] = "SCHEDULED"
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                }
                
                call.respond(HttpStatusCode.Created, mapOf("id" to interviewId))
            }
            
            // ===== Reminders =====
            
            // Get upcoming reminders across all applications
            get("/reminders/upcoming") {
                val userId = call.principal<JWTPrincipal>()?.subject
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                
                val now = Clock.System.now()
                
                val reminders = transaction {
                    // Get reminders that haven't been completed and are in the future
                    (JobApplicationRemindersTable innerJoin JobApplicationsTable)
                        .select {
                            (JobApplicationsTable.userId eq userId) and
                            (JobApplicationRemindersTable.isCompleted eq false) and
                            (JobApplicationRemindersTable.reminderAt greaterEq now)
                        }
                        .orderBy(JobApplicationRemindersTable.reminderAt, SortOrder.ASC)
                        .limit(limit)
                        .map { row ->
                            UpcomingReminderResponse(
                                id = row[JobApplicationRemindersTable.id],
                                applicationId = row[JobApplicationRemindersTable.applicationId],
                                applicationTitle = row[JobApplicationsTable.jobTitle],
                                companyName = row[JobApplicationsTable.companyName],
                                reminderType = row[JobApplicationRemindersTable.reminderType],
                                title = row[JobApplicationRemindersTable.title],
                                message = row[JobApplicationRemindersTable.message],
                                scheduledAt = row[JobApplicationRemindersTable.reminderAt].toString()
                            )
                        }
                }
                
                call.respond(reminders)
            }
            
            // ===== Statistics =====
            
            // Get tracker statistics
            get("/stats") {
                val userId = call.principal<JWTPrincipal>()?.subject
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                
                val stats = transaction {
                    val applications = JobApplicationsTable
                        .select { JobApplicationsTable.userId eq userId }
                        .toList()
                    
                    val total = applications.size
                    val saved = applications.count { it[JobApplicationsTable.status] == "SAVED" }
                    val applied = applications.count { it[JobApplicationsTable.status] != "SAVED" }
                    val interviews = applications.count { 
                        it[JobApplicationsTable.status] in listOf("PHONE_SCREEN", "INTERVIEW", "FINAL_ROUND") 
                    }
                    val offers = applications.count { 
                        it[JobApplicationsTable.status] in listOf("OFFER", "NEGOTIATING", "ACCEPTED") 
                    }
                    val rejected = applications.count { it[JobApplicationsTable.status] == "REJECTED" }
                    
                    TrackerStatsResponse(
                        totalApplications = total,
                        savedCount = saved,
                        appliedCount = applied,
                        interviewCount = interviews,
                        offerCount = offers,
                        rejectedCount = rejected,
                        interviewRate = if (applied > 0) (interviews.toFloat() / applied * 100) else 0f,
                        offerRate = if (interviews > 0) (offers.toFloat() / interviews * 100) else 0f
                    )
                }
                
                call.respond(stats)
            }
        }
    }
}

// ===== Request/Response DTOs =====

@Serializable
data class CreateJobApplicationRequest(
    val resumeId: String? = null,
    val coverLetterId: String? = null,
    val jobTitle: String,
    val companyName: String,
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
data class CreateJobApplicationResponse(
    val id: String,
    val message: String
)

@Serializable
data class QuickSaveJobRequest(
    val jobTitle: String,
    val companyName: String,
    val jobUrl: String? = null,
    val jobBoardSource: String? = null,
    val externalJobId: String? = null
)

@Serializable
data class QuickSaveJobResponse(
    val id: String,
    val message: String,
    val alreadyExists: Boolean
)

@Serializable
data class UpdateJobApplicationRequest(
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
data class JobTrackerStatusUpdateRequest(
    val status: String,
    val notes: String? = null
)

@Serializable
data class CreateNoteRequest(
    val content: String,
    val noteType: String? = null
)

@Serializable
data class CreateReminderRequest(
    val reminderType: String,
    val title: String,
    val message: String? = null,
    val reminderAt: String
)

@Serializable
data class CreateInterviewRequest(
    val interviewType: String,
    val scheduledAt: String,
    val durationMinutes: Int? = null,
    val location: String? = null,
    val interviewerName: String? = null,
    val interviewerTitle: String? = null,
    val notes: String? = null
)

@Serializable
data class JobApplicationsListResponse(
    val applications: List<JobApplicationResponse>,
    val total: Int,
    val limit: Int,
    val offset: Int
)

@Serializable
data class JobApplicationResponse(
    val id: String,
    val jobTitle: String,
    val companyName: String,
    val companyLogo: String?,
    val jobUrl: String?,
    val jobBoardSource: String?,
    val city: String?,
    val province: String?,
    val isRemote: Boolean,
    val isHybrid: Boolean,
    val salaryMin: Int?,
    val salaryMax: Int?,
    val salaryCurrency: String,
    val salaryPeriod: String?,
    val status: String,
    val appliedAt: String?,
    val nocCode: String?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class JobApplicationDetailResponse(
    val application: JobApplicationResponse,
    val notes: List<NoteResponse>,
    val reminders: List<ReminderResponse>,
    val interviews: List<InterviewResponse>,
    val statusHistory: List<StatusChangeResponse>
)

@Serializable
data class NoteResponse(
    val id: String,
    val content: String,
    val noteType: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ReminderResponse(
    val id: String,
    val reminderType: String,
    val title: String,
    val message: String?,
    val reminderAt: String,
    val isCompleted: Boolean,
    val completedAt: String?
)

@Serializable
data class UpcomingReminderResponse(
    val id: String,
    val applicationId: String,
    val applicationTitle: String,
    val companyName: String,
    val reminderType: String,
    val title: String,
    val message: String?,
    val scheduledAt: String
)

@Serializable
data class InterviewResponse(
    val id: String,
    val interviewType: String,
    val scheduledAt: String,
    val durationMinutes: Int?,
    val location: String?,
    val interviewerName: String?,
    val interviewerTitle: String?,
    val status: String
)

@Serializable
data class StatusChangeResponse(
    val id: String,
    val fromStatus: String?,
    val toStatus: String,
    val notes: String?,
    val changedAt: String
)

@Serializable
data class TrackerStatsResponse(
    val totalApplications: Int,
    val savedCount: Int,
    val appliedCount: Int,
    val interviewCount: Int,
    val offerCount: Int,
    val rejectedCount: Int,
    val interviewRate: Float,
    val offerRate: Float
)

// ===== Extension functions for mapping =====

private fun ResultRow.toJobApplicationResponse() = JobApplicationResponse(
    id = this[JobApplicationsTable.id],
    jobTitle = this[JobApplicationsTable.jobTitle],
    companyName = this[JobApplicationsTable.companyName],
    companyLogo = this[JobApplicationsTable.companyLogo],
    jobUrl = this[JobApplicationsTable.jobUrl],
    jobBoardSource = this[JobApplicationsTable.jobBoardSource],
    city = this[JobApplicationsTable.city],
    province = this[JobApplicationsTable.province],
    isRemote = this[JobApplicationsTable.isRemote],
    isHybrid = this[JobApplicationsTable.isHybrid],
    salaryMin = this[JobApplicationsTable.salaryMin],
    salaryMax = this[JobApplicationsTable.salaryMax],
    salaryCurrency = this[JobApplicationsTable.salaryCurrency],
    salaryPeriod = this[JobApplicationsTable.salaryPeriod],
    status = this[JobApplicationsTable.status],
    appliedAt = this[JobApplicationsTable.appliedAt]?.toString(),
    nocCode = this[JobApplicationsTable.nocCode],
    createdAt = this[JobApplicationsTable.createdAt].toString(),
    updatedAt = this[JobApplicationsTable.updatedAt].toString()
)

private fun ResultRow.toNoteResponse() = NoteResponse(
    id = this[JobApplicationNotesTable.id],
    content = this[JobApplicationNotesTable.content],
    noteType = this[JobApplicationNotesTable.noteType],
    createdAt = this[JobApplicationNotesTable.createdAt].toString(),
    updatedAt = this[JobApplicationNotesTable.updatedAt].toString()
)

private fun ResultRow.toReminderResponse() = ReminderResponse(
    id = this[JobApplicationRemindersTable.id],
    reminderType = this[JobApplicationRemindersTable.reminderType],
    title = this[JobApplicationRemindersTable.title],
    message = this[JobApplicationRemindersTable.message],
    reminderAt = this[JobApplicationRemindersTable.reminderAt].toString(),
    isCompleted = this[JobApplicationRemindersTable.isCompleted],
    completedAt = this[JobApplicationRemindersTable.completedAt]?.toString()
)

private fun ResultRow.toInterviewResponse() = InterviewResponse(
    id = this[JobApplicationInterviewsTable.id],
    interviewType = this[JobApplicationInterviewsTable.interviewType],
    scheduledAt = this[JobApplicationInterviewsTable.scheduledAt].toString(),
    durationMinutes = this[JobApplicationInterviewsTable.duration],
    location = this[JobApplicationInterviewsTable.location],
    interviewerName = this[JobApplicationInterviewsTable.interviewerName],
    interviewerTitle = this[JobApplicationInterviewsTable.interviewerTitle],
    status = this[JobApplicationInterviewsTable.status]
)

private fun ResultRow.toStatusChangeResponse() = StatusChangeResponse(
    id = this[JobApplicationStatusHistoryTable.id],
    fromStatus = this[JobApplicationStatusHistoryTable.fromStatus],
    toStatus = this[JobApplicationStatusHistoryTable.toStatus],
    notes = this[JobApplicationStatusHistoryTable.notes],
    changedAt = this[JobApplicationStatusHistoryTable.changedAt].toString()
)
