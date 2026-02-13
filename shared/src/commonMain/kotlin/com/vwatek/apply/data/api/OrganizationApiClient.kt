package com.vwatek.apply.data.api

import com.vwatek.apply.domain.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

/**
 * Phase 5: Organization/Enterprise API Client
 * Handles team management, invitations, templates, and analytics
 */
class OrganizationApiClient(
    private val httpClient: HttpClient
) {
    private val baseUrl = "${ApiConfig.apiV1Url}/organizations"
    
    // ===== Organization Management =====
    
    /**
     * Create a new organization
     */
    suspend fun createOrganization(request: CreateOrganizationApiRequest): Result<OrganizationResponse> {
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
    
    /**
     * Get user's organizations
     */
    suspend fun getOrganizations(): Result<OrganizationsListResponse> {
        return try {
            val response = httpClient.get(baseUrl)
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get a specific organization
     */
    suspend fun getOrganization(orgId: String): Result<OrganizationResponse> {
        return try {
            val response = httpClient.get("$baseUrl/$orgId")
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update organization details
     */
    suspend fun updateOrganization(
        orgId: String,
        request: UpdateOrganizationApiRequest
    ): Result<MessageResponse> {
        return try {
            val response = httpClient.put("$baseUrl/$orgId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ===== Member Management =====
    
    /**
     * Get organization members
     */
    suspend fun getMembers(orgId: String): Result<MembersListResponse> {
        return try {
            val response = httpClient.get("$baseUrl/$orgId/members")
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Invite a member to the organization
     */
    suspend fun inviteMember(
        orgId: String,
        email: String,
        role: String = "MEMBER"
    ): Result<InvitationResponse> {
        return try {
            val response = httpClient.post("$baseUrl/$orgId/members/invite") {
                contentType(ContentType.Application.Json)
                setBody(InviteMemberApiRequest(email, role))
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update a member's role
     */
    suspend fun updateMemberRole(
        orgId: String,
        memberId: String,
        role: String
    ): Result<MessageResponse> {
        return try {
            val response = httpClient.put("$baseUrl/$orgId/members/$memberId/role") {
                contentType(ContentType.Application.Json)
                setBody(UpdateMemberRoleApiRequest(role))
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Remove a member from the organization
     */
    suspend fun removeMember(orgId: String, memberId: String): Result<MessageResponse> {
        return try {
            val response = httpClient.delete("$baseUrl/$orgId/members/$memberId")
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ===== Templates =====
    
    /**
     * Get organization templates
     */
    suspend fun getTemplates(orgId: String): Result<TemplatesListResponse> {
        return try {
            val response = httpClient.get("$baseUrl/$orgId/templates")
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a new template
     */
    suspend fun createTemplate(
        orgId: String,
        request: CreateTemplateApiRequest
    ): Result<TemplateResponse> {
        return try {
            val response = httpClient.post("$baseUrl/$orgId/templates") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ===== Analytics =====
    
    /**
     * Get organization analytics
     */
    suspend fun getAnalytics(orgId: String): Result<AnalyticsResponse> {
        return try {
            val response = httpClient.get("$baseUrl/$orgId/analytics")
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ===== Activity Log =====
    
    /**
     * Get organization activity log
     */
    suspend fun getActivityLog(orgId: String, limit: Int = 50): Result<ActivityLogResponse> {
        return try {
            val response = httpClient.get("$baseUrl/$orgId/activity") {
                parameter("limit", limit)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ===== Invitations =====
    
    /**
     * Accept an organization invitation
     */
    suspend fun acceptInvitation(token: String): Result<MessageResponse> {
        return try {
            val response = httpClient.post("${ApiConfig.apiV1Url}/invitations/$token/accept")
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ===== Request DTOs =====

@Serializable
data class CreateOrganizationApiRequest(
    val name: String,
    val description: String? = null,
    val industry: String? = null,
    val size: String? = null
)

@Serializable
data class UpdateOrganizationApiRequest(
    val name: String? = null,
    val description: String? = null,
    val logoUrl: String? = null,
    val industry: String? = null,
    val size: String? = null
)

@Serializable
data class InviteMemberApiRequest(
    val email: String,
    val role: String = "MEMBER"
)

@Serializable
data class UpdateMemberRoleApiRequest(
    val role: String
)

@Serializable
data class CreateTemplateApiRequest(
    val name: String,
    val description: String? = null,
    val type: String,
    val content: String,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val isDefault: Boolean = false
)

// ===== Response DTOs =====

@Serializable
data class OrganizationResponse(
    val organization: OrganizationDto
)

@Serializable
data class OrganizationsListResponse(
    val organizations: List<OrganizationDto>
)

@Serializable
data class OrganizationDto(
    val id: String,
    val name: String,
    val slug: String,
    val description: String?,
    val logoUrl: String?,
    val industry: String?,
    val size: String?,
    val subscriptionTier: String,
    val ssoEnabled: Boolean,
    val memberCount: Int,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class MemberResponse(
    val member: MemberDto
)

@Serializable
data class MembersListResponse(
    val members: List<MemberDto>
)

@Serializable
data class MemberDto(
    val id: String,
    val userId: String,
    val email: String?,
    val name: String?,
    val role: String,
    val status: String,
    val joinedAt: String?
)

@Serializable
data class InvitationResponse(
    val invitation: InvitationDto
)

@Serializable
data class InvitationsListResponse(
    val invitations: List<InvitationDto>
)

@Serializable
data class InvitationDto(
    val id: String,
    val email: String,
    val role: String,
    val status: String,
    val expiresAt: String,
    val createdAt: String
)

@Serializable
data class TemplateResponse(
    val template: TemplateDto
)

@Serializable
data class TemplatesListResponse(
    val templates: List<TemplateDto>
)

@Serializable
data class TemplateDto(
    val id: String,
    val name: String,
    val description: String?,
    val type: String,
    val category: String?,
    val tags: List<String>,
    val isDefault: Boolean,
    val createdAt: String
)

@Serializable
data class AnalyticsResponse(
    val analytics: AnalyticsDto
)

@Serializable
data class AnalyticsDto(
    val totalMembers: Int,
    val activeMembers: Int,
    val totalApplications: Int,
    val totalInterviews: Int,
    val totalOffers: Int,
    val successRate: Double,
    val periodStart: String,
    val periodEnd: String
)

@Serializable
data class ActivityLogResponse(
    val activities: List<ActivityDto>
)

@Serializable
data class ActivityDto(
    val id: String,
    val userId: String,
    val userName: String?,
    val action: String,
    val resourceType: String,
    val details: String?,
    val timestamp: String
)
