package com.vwatek.apply.data.api

import com.vwatek.apply.privacy.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

/**
 * Privacy API Client for VwaTek Apply
 * 
 * Handles communication with the /api/privacy endpoints
 * Implements PIPEDA-compliant data access and consent management
 */
class PrivacyApiClient(
    private val httpClient: HttpClient,
    private val getAuthToken: () -> String?
) {
    
    // ========== API DTO Models (match backend exactly) ==========
    
    @Serializable
    data class ApiConsentUpdateRequest(
        val purpose: String,
        val granted: Boolean
    )
    
    @Serializable
    data class ApiConsentStatusResponse(
        val purpose: String,
        val status: String,
        val grantedAt: Long?,
        val policyVersion: String
    )
    
    @Serializable
    data class ApiAllConsentsResponse(
        val consents: List<ApiConsentStatusResponse>,
        val policyVersion: String,
        val lastUpdated: Long?
    )
    
    @Serializable
    data class ApiDataAccessRequestBody(
        val requestType: String,
        val reason: String?
    )
    
    @Serializable
    data class ApiDataAccessRequestResponse(
        val id: String,
        val requestType: String,
        val status: String,
        val requestedAt: Long,
        val estimatedCompletionDays: Int
    )
    
    @Serializable
    data class ApiDataExportResponse(
        val downloadUrl: String?,
        val expiresAt: Long?,
        val status: String,
        val includesData: List<String>
    )
    
    @Serializable
    data class ApiUserDataSummary(
        val userId: String,
        val email: String,
        val createdAt: Long,
        val resumes: List<ApiResumeSummary>,
        val coverLetters: List<ApiCoverLetterSummary>,
        val interviewSessions: Int,
        val consents: List<ApiConsentStatusResponse>,
        val dataRegion: String
    )
    
    @Serializable
    data class ApiResumeSummary(
        val id: String,
        val name: String,
        val createdAt: Long
    )
    
    @Serializable
    data class ApiCoverLetterSummary(
        val id: String,
        val jobTitle: String,
        val company: String,
        val createdAt: Long
    )
    
    // ========== Domain Models for Client Use ==========
    
    data class ConsentStatusResult(
        val purpose: String,
        val status: ConsentStatus,
        val grantedAt: Long?,
        val policyVersion: String
    )
    
    data class AllConsentsResult(
        val consents: List<ConsentStatusResult>,
        val policyVersion: String,
        val lastUpdated: Long?
    )
    
    data class DataAccessRequestResult(
        val id: String,
        val requestType: DataRequestType,
        val status: DataRequestStatus,
        val requestedAt: Long,
        val estimatedCompletionDays: Int
    )
    
    enum class DataRequestType {
        ACCESS,
        EXPORT,
        DELETION,
        CORRECTION
    }
    
    enum class DataRequestStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        REJECTED
    }
    
    data class UserDataSummaryResult(
        val userId: String,
        val email: String,
        val createdAt: Long,
        val resumeCount: Int,
        val coverLetterCount: Int,
        val interviewSessionCount: Int,
        val consents: List<ConsentStatusResult>,
        val dataRegion: String
    )
    
    // ========== API Methods ==========
    
    /**
     * Get all consent statuses for current user
     */
    suspend fun getAllConsents(): Result<AllConsentsResult> {
        return runCatching {
            val token = getAuthToken() ?: throw IllegalStateException("No auth token available")
            
            val response: ApiAllConsentsResponse = httpClient.get("${ApiConfig.privacyApiUrl}/consent") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
            
            AllConsentsResult(
                consents = response.consents.map { it.toDomain() },
                policyVersion = response.policyVersion,
                lastUpdated = response.lastUpdated
            )
        }
    }
    
    /**
     * Update consent for a specific purpose
     */
    suspend fun updateConsent(purpose: ConsentPurpose, granted: Boolean): Result<ConsentStatusResult> {
        return runCatching {
            val token = getAuthToken() ?: throw IllegalStateException("No auth token available")
            
            val request = ApiConsentUpdateRequest(
                purpose = purpose.name,
                granted = granted
            )
            
            val response: ApiConsentStatusResponse = httpClient.post("${ApiConfig.privacyApiUrl}/consent") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }.body()
            
            response.toDomain()
        }
    }
    
    /**
     * Submit a data access request (ACCESS, EXPORT, DELETION, CORRECTION)
     */
    suspend fun submitDataRequest(
        requestType: DataRequestType,
        reason: String? = null
    ): Result<DataAccessRequestResult> {
        return runCatching {
            val token = getAuthToken() ?: throw IllegalStateException("No auth token available")
            
            val request = ApiDataAccessRequestBody(
                requestType = requestType.name,
                reason = reason
            )
            
            val response: ApiDataAccessRequestResponse = httpClient.post("${ApiConfig.privacyApiUrl}/data-request") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }.body()
            
            DataAccessRequestResult(
                id = response.id,
                requestType = DataRequestType.valueOf(response.requestType),
                status = DataRequestStatus.valueOf(response.status),
                requestedAt = response.requestedAt,
                estimatedCompletionDays = response.estimatedCompletionDays
            )
        }
    }
    
    /**
     * Get all data access requests for current user
     */
    suspend fun getDataRequests(): Result<List<DataAccessRequestResult>> {
        return runCatching {
            val token = getAuthToken() ?: throw IllegalStateException("No auth token available")
            
            val response: List<ApiDataAccessRequestResponse> = httpClient.get("${ApiConfig.privacyApiUrl}/data-requests") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
            
            response.map {
                DataAccessRequestResult(
                    id = it.id,
                    requestType = DataRequestType.valueOf(it.requestType),
                    status = DataRequestStatus.valueOf(it.status),
                    requestedAt = it.requestedAt,
                    estimatedCompletionDays = it.estimatedCompletionDays
                )
            }
        }
    }
    
    /**
     * Get summary of user's data
     */
    suspend fun getUserDataSummary(): Result<UserDataSummaryResult> {
        return runCatching {
            val token = getAuthToken() ?: throw IllegalStateException("No auth token available")
            
            val response: ApiUserDataSummary = httpClient.get("${ApiConfig.privacyApiUrl}/my-data/summary") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
            
            UserDataSummaryResult(
                userId = response.userId,
                email = response.email,
                createdAt = response.createdAt,
                resumeCount = response.resumes.size,
                coverLetterCount = response.coverLetters.size,
                interviewSessionCount = response.interviewSessions,
                consents = response.consents.map { it.toDomain() },
                dataRegion = response.dataRegion
            )
        }
    }
    
    /**
     * Request data deletion (PIPEDA right to erasure)
     */
    suspend fun requestDataDeletion(reason: String? = null): Result<DataAccessRequestResult> {
        return submitDataRequest(DataRequestType.DELETION, reason)
    }
    
    /**
     * Request data export (PIPEDA right to access)
     */
    suspend fun requestDataExport(): Result<DataAccessRequestResult> {
        return submitDataRequest(DataRequestType.EXPORT, null)
    }
    
    // ========== Conversion Functions ==========
    
    private fun ApiConsentStatusResponse.toDomain(): ConsentStatusResult {
        return ConsentStatusResult(
            purpose = purpose,
            status = when (status.uppercase()) {
                "GRANTED" -> ConsentStatus.GRANTED
                "DENIED" -> ConsentStatus.DENIED
                "WITHDRAWN" -> ConsentStatus.WITHDRAWN
                else -> ConsentStatus.NOT_SET
            },
            grantedAt = grantedAt,
            policyVersion = policyVersion
        )
    }
}
