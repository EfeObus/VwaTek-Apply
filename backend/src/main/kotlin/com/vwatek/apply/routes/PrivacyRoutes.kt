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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/**
 * Privacy API Routes for PIPEDA Compliance
 * 
 * Provides endpoints for:
 * - Consent management
 * - Data access requests
 * - Data export
 * - Data deletion
 */

// Request/Response models
@Serializable
data class ConsentUpdateRequest(
    val purpose: String,
    val granted: Boolean
)

@Serializable
data class ConsentStatusResponse(
    val purpose: String,
    val status: String,
    val grantedAt: Long?,
    val policyVersion: String
)

@Serializable
data class AllConsentsResponse(
    val consents: List<ConsentStatusResponse>,
    val policyVersion: String,
    val lastUpdated: Long?
)

@Serializable
data class DataAccessRequestBody(
    val requestType: String, // ACCESS, EXPORT, DELETION, CORRECTION
    val reason: String?
)

@Serializable
data class DataAccessRequestResponse(
    val id: String,
    val requestType: String,
    val status: String,
    val requestedAt: Long,
    val estimatedCompletionDays: Int
)

@Serializable
data class DataExportResponse(
    val downloadUrl: String?,
    val expiresAt: Long?,
    val status: String,
    val includesData: List<String>
)

@Serializable
data class UserDataSummary(
    val userId: String,
    val email: String,
    val createdAt: Long,
    val resumes: List<ResumeSummary>,
    val coverLetters: List<CoverLetterSummary>,
    val interviewSessions: Int,
    val consents: List<ConsentStatusResponse>,
    val dataRegion: String
)

@Serializable
data class ResumeSummary(
    val id: String,
    val name: String,
    val createdAt: Long
)

@Serializable
data class CoverLetterSummary(
    val id: String,
    val jobTitle: String,
    val company: String,
    val createdAt: Long
)

fun Route.privacyRoutes() {
    route("/api/privacy") {
        
        // Get all consent statuses
        authenticate("auth-jwt") {
            get("/consent") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                
                val consents = transaction {
                    ConsentRecordsTable.select { ConsentRecordsTable.userId eq userId }
                        .map { row ->
                            ConsentStatusResponse(
                                purpose = row[ConsentRecordsTable.purpose],
                                status = row[ConsentRecordsTable.status],
                                grantedAt = row[ConsentRecordsTable.grantedAt]?.toEpochMilliseconds(),
                                policyVersion = row[ConsentRecordsTable.policyVersion]
                            )
                        }
                }
                
                val lastUpdated = consents.maxOfOrNull { it.grantedAt ?: 0L }
                
                call.respond(AllConsentsResponse(
                    consents = consents,
                    policyVersion = CURRENT_POLICY_VERSION,
                    lastUpdated = lastUpdated
                ))
            }
            
            // Update consent status
            post("/consent") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                
                val request = call.receive<ConsentUpdateRequest>()
                val ipAddress = call.request.local.remoteHost
                val userAgent = call.request.userAgent()
                
                val newStatus = if (request.granted) "GRANTED" else "DENIED"
                val now = kotlinx.datetime.Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
                
                transaction {
                    // Get existing record if any
                    val existing = ConsentRecordsTable.select {
                        (ConsentRecordsTable.userId eq userId) and (ConsentRecordsTable.purpose eq request.purpose)
                    }.singleOrNull()
                    
                    val previousStatus = existing?.get(ConsentRecordsTable.status)
                    
                    if (existing != null) {
                        // Update existing
                        ConsentRecordsTable.update({
                            (ConsentRecordsTable.userId eq userId) and (ConsentRecordsTable.purpose eq request.purpose)
                        }) {
                            it[status] = newStatus
                            it[policyVersion] = CURRENT_POLICY_VERSION
                            it[updatedAt] = now
                            if (request.granted) {
                                it[grantedAt] = now
                                it[withdrawnAt] = null
                            } else {
                                it[withdrawnAt] = now
                            }
                            it[ConsentRecordsTable.ipAddress] = ipAddress
                            it[ConsentRecordsTable.userAgent] = userAgent
                        }
                    } else {
                        // Insert new
                        ConsentRecordsTable.insert {
                            it[id] = UUID.randomUUID().toString()
                            it[ConsentRecordsTable.userId] = userId
                            it[purpose] = request.purpose
                            it[status] = newStatus
                            it[policyVersion] = CURRENT_POLICY_VERSION
                            it[createdAt] = now
                            it[updatedAt] = now
                            if (request.granted) {
                                it[grantedAt] = now
                            }
                            it[ConsentRecordsTable.ipAddress] = ipAddress
                            it[ConsentRecordsTable.userAgent] = userAgent
                        }
                    }
                    
                    // Add audit log entry
                    ConsentAuditLogTable.insert {
                        it[ConsentAuditLogTable.userId] = userId
                        it[ConsentAuditLogTable.purpose] = request.purpose
                        it[action] = if (request.granted) "GRANT" else "WITHDRAW"
                        it[ConsentAuditLogTable.previousStatus] = previousStatus
                        it[ConsentAuditLogTable.newStatus] = newStatus
                        it[policyVersion] = CURRENT_POLICY_VERSION
                        it[ConsentAuditLogTable.ipAddress] = ipAddress
                        it[ConsentAuditLogTable.userAgent] = userAgent
                        it[timestamp] = now
                    }
                }
                
                call.respond(HttpStatusCode.OK, mapOf(
                    "purpose" to request.purpose,
                    "status" to newStatus,
                    "updatedAt" to Clock.System.now().toEpochMilliseconds()
                ))
            }
        }
        
        // Data access request endpoints
        authenticate("auth-jwt") {
            // Submit a data access request
            post("/data-request") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                
                val request = call.receive<DataAccessRequestBody>()
                val now = kotlinx.datetime.Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
                val requestId = UUID.randomUUID().toString()
                
                // Validate request type
                val validTypes = listOf("ACCESS", "EXPORT", "DELETION", "CORRECTION")
                if (request.requestType !in validTypes) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest, 
                        mapOf("error" to "Invalid request type. Must be one of: $validTypes")
                    )
                }
                
                transaction {
                    DataAccessRequestsTable.insert {
                        it[id] = requestId
                        it[DataAccessRequestsTable.userId] = userId
                        it[requestType] = request.requestType
                        it[status] = "PENDING"
                        it[reason] = request.reason
                        it[requestedAt] = now
                    }
                }
                
                val estimatedDays = when (request.requestType) {
                    "ACCESS" -> 3
                    "EXPORT" -> 5
                    "DELETION" -> 30  // PIPEDA allows up to 30 days
                    "CORRECTION" -> 7
                    else -> 30
                }
                
                call.respond(HttpStatusCode.Created, DataAccessRequestResponse(
                    id = requestId,
                    requestType = request.requestType,
                    status = "PENDING",
                    requestedAt = now.toEpochMilliseconds(),
                    estimatedCompletionDays = estimatedDays
                ))
            }
            
            // Get data access request status
            get("/data-request/{id}") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                
                val requestId = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Request ID required"))
                
                val request = transaction {
                    DataAccessRequestsTable.select {
                        (DataAccessRequestsTable.id eq requestId) and (DataAccessRequestsTable.userId eq userId)
                    }.singleOrNull()
                }
                
                if (request == null) {
                    return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Request not found"))
                }
                
                call.respond(mapOf(
                    "id" to requestId,
                    "requestType" to request[DataAccessRequestsTable.requestType],
                    "status" to request[DataAccessRequestsTable.status],
                    "requestedAt" to request[DataAccessRequestsTable.requestedAt].toEpochMilliseconds(),
                    "processedAt" to request[DataAccessRequestsTable.processedAt]?.toEpochMilliseconds(),
                    "completedAt" to request[DataAccessRequestsTable.completedAt]?.toEpochMilliseconds(),
                    "dataUrl" to request[DataAccessRequestsTable.dataUrl],
                    "dataExpiresAt" to request[DataAccessRequestsTable.dataExpiresAt]?.toEpochMilliseconds()
                ))
            }
            
            // Get all data requests for user
            get("/data-requests") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                
                val requests = transaction {
                    DataAccessRequestsTable.select { DataAccessRequestsTable.userId eq userId }
                        .orderBy(DataAccessRequestsTable.requestedAt, SortOrder.DESC)
                        .map { row ->
                            mapOf(
                                "id" to row[DataAccessRequestsTable.id],
                                "requestType" to row[DataAccessRequestsTable.requestType],
                                "status" to row[DataAccessRequestsTable.status],
                                "requestedAt" to row[DataAccessRequestsTable.requestedAt].toEpochMilliseconds()
                            )
                        }
                }
                
                call.respond(mapOf("requests" to requests))
            }
            
            // Get user data summary (what data we have)
            get("/my-data/summary") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                
                val summary = transaction {
                    val user = UsersTable.select { UsersTable.id eq userId }.singleOrNull()
                        ?: return@transaction null
                    
                    val resumes = ResumesTable.select { ResumesTable.userId eq userId }
                        .map { ResumeSummary(
                            id = it[ResumesTable.id],
                            name = it[ResumesTable.name],
                            createdAt = it[ResumesTable.createdAt].toEpochMilliseconds()
                        )}
                    
                    val coverLetters = CoverLettersTable.select { CoverLettersTable.userId eq userId }
                        .map { CoverLetterSummary(
                            id = it[CoverLettersTable.id],
                            jobTitle = it[CoverLettersTable.jobTitle],
                            company = it[CoverLettersTable.companyName],
                            createdAt = it[CoverLettersTable.createdAt].toEpochMilliseconds()
                        )}
                    
                    val interviewCount = InterviewSessionsTable.select { 
                        InterviewSessionsTable.userId eq userId 
                    }.count().toInt()
                    
                    val consents = ConsentRecordsTable.select { ConsentRecordsTable.userId eq userId }
                        .map { ConsentStatusResponse(
                            purpose = it[ConsentRecordsTable.purpose],
                            status = it[ConsentRecordsTable.status],
                            grantedAt = it[ConsentRecordsTable.grantedAt]?.toEpochMilliseconds(),
                            policyVersion = it[ConsentRecordsTable.policyVersion]
                        )}
                    
                    val dataRegion = UserDataRegionsTable.select { 
                        UserDataRegionsTable.userId eq userId 
                    }.firstOrNull()?.get(UserDataRegionsTable.region) ?: "northamerica-northeast1"
                    
                    UserDataSummary(
                        userId = userId,
                        email = user[UsersTable.email],
                        createdAt = user[UsersTable.createdAt].toEpochMilliseconds(),
                        resumes = resumes,
                        coverLetters = coverLetters,
                        interviewSessions = interviewCount,
                        consents = consents,
                        dataRegion = dataRegion
                    )
                }
                
                if (summary != null) {
                    call.respond(summary)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                }
            }
            
            // Request account deletion
            delete("/my-data") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                
                val now = kotlinx.datetime.Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
                val requestId = UUID.randomUUID().toString()
                
                transaction {
                    // Create deletion request
                    DataAccessRequestsTable.insert {
                        it[id] = requestId
                        it[DataAccessRequestsTable.userId] = userId
                        it[requestType] = "DELETION"
                        it[status] = "PENDING"
                        it[reason] = "User requested account deletion"
                        it[requestedAt] = now
                    }
                }
                
                call.respond(HttpStatusCode.Accepted, mapOf(
                    "message" to "Account deletion request submitted",
                    "requestId" to requestId,
                    "estimatedCompletionDays" to 30,
                    "note" to "Per PIPEDA requirements, your data will be deleted within 30 days. You will receive a confirmation email when complete."
                ))
            }
        }
    }
}

private const val CURRENT_POLICY_VERSION = "1.0.0"
