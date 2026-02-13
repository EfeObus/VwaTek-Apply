package com.vwatek.apply.routes

import com.vwatek.apply.auth.requireAuth
import com.vwatek.apply.db.tables.OrganizationsTable
import com.vwatek.apply.db.tables.OrganizationMembersTable
import com.vwatek.apply.db.tables.OrganizationInvitationsTable
import com.vwatek.apply.db.tables.OrganizationTemplatesTable
import com.vwatek.apply.db.tables.OrganizationActivityLogTable
import com.vwatek.apply.db.tables.UsersTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

/**
 * Phase 5: Organization/Enterprise Routes
 * Team management, SSO, and admin features
 * 
 * Note: Table definitions are in db/tables/EnterpriseTables.kt
 */

// ===== Organization Settings Table (local to routes for now) =====
object OrganizationSettingsTable : Table("organization_settings") {
    val id = varchar("id", 36)
    val organizationId = varchar("organization_id", 36).references(OrganizationsTable.id)
    val allowMemberInvites = bool("allow_member_invites").default(true)
    val requireApprovalForJoin = bool("require_approval_for_join").default(true)
    val defaultMemberRole = varchar("default_member_role", 50).default("MEMBER")
    val shareTemplatesWithMembers = bool("share_templates_with_members").default(true)
    val allowExternalSharing = bool("allow_external_sharing").default(false)
    val requireMfa = bool("require_mfa").default(false)
    val updatedAt = long("updated_at")
    override val primaryKey = PrimaryKey(id)
}

// ===== Request/Response DTOs =====

@Serializable
data class CreateOrganizationRequest(
    val name: String,
    val description: String? = null,
    val industry: String? = null,
    val size: String? = null
)

@Serializable
data class UpdateOrganizationRequest(
    val name: String? = null,
    val description: String? = null,
    val logoUrl: String? = null,
    val industry: String? = null,
    val size: String? = null
)

@Serializable
data class InviteMemberRequest(
    val email: String,
    val role: String = "MEMBER"
)

@Serializable
data class BulkInviteRequest(
    val emails: List<String>,
    val role: String = "MEMBER"
)

@Serializable
data class UpdateMemberRoleRequest(
    val role: String
)

@Serializable
data class CreateTemplateRequest(
    val name: String,
    val description: String? = null,
    val type: String,
    val content: String,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val isDefault: Boolean = false
)

@Serializable
data class OrganizationResponse(
    val organization: OrganizationDto
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

// ===== Routes =====

fun Route.organizationRoutes() {
    route("/organizations") {
        
        // Create organization
        post {
            val userId = call.requireAuth() ?: return@post
            val request = call.receive<CreateOrganizationRequest>()
            
            if (request.name.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Organization name is required"))
                return@post
            }
            
            val slug = request.name.lowercase().replace(Regex("[^a-z0-9]+"), "-")
            val now = Instant.now().toEpochMilli()
            
            val orgId = transaction {
                // Check if slug exists
                val existingSlug = OrganizationsTable.select { OrganizationsTable.slug eq slug }.count()
                if (existingSlug > 0) {
                    return@transaction null
                }
                
                val orgId = OrganizationsTable.insert {
                    it[name] = request.name
                    it[OrganizationsTable.slug] = slug
                    it[description] = request.description
                    it[industry] = request.industry
                    it[size] = request.size
                    it[createdAt] = now
                    it[updatedAt] = now
                } get OrganizationsTable.id
                
                // Create default settings
                OrganizationSettingsTable.insert {
                    it[organizationId] = orgId
                    it[updatedAt] = now
                }
                
                // Add creator as owner
                OrganizationMembersTable.insert {
                    it[organizationId] = orgId
                    it[OrganizationMembersTable.userId] = UUID.fromString(userId)
                    it[role] = "OWNER"
                    it[status] = "ACTIVE"
                    it[joinedAt] = now
                    it[createdAt] = now
                    it[updatedAt] = now
                }
                
                orgId
            }
            
            if (orgId == null) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "Organization with similar name already exists"))
                return@post
            }
            
            call.respond(HttpStatusCode.Created, OrganizationResponse(
                organization = OrganizationDto(
                    id = orgId.toString(),
                    name = request.name,
                    slug = slug,
                    description = request.description,
                    logoUrl = null,
                    industry = request.industry,
                    size = request.size,
                    subscriptionTier = "FREE",
                    ssoEnabled = false,
                    memberCount = 1,
                    createdAt = Instant.ofEpochMilli(now).toString(),
                    updatedAt = Instant.ofEpochMilli(now).toString()
                )
            ))
        }
        
        // Get user's organizations
        get {
            val userId = call.requireAuth() ?: return@get
            
            val orgs = transaction {
                (OrganizationsTable innerJoin OrganizationMembersTable)
                    .select { 
                        (OrganizationMembersTable.userId eq UUID.fromString(userId)) and
                        (OrganizationMembersTable.status eq "ACTIVE")
                    }
                    .map { row ->
                        val orgId = row[OrganizationsTable.id]
                        val memberCount = OrganizationMembersTable
                            .select { (OrganizationMembersTable.organizationId eq orgId) and (OrganizationMembersTable.status eq "ACTIVE") }
                            .count()
                        
                        OrganizationDto(
                            id = orgId.toString(),
                            name = row[OrganizationsTable.name],
                            slug = row[OrganizationsTable.slug],
                            description = row[OrganizationsTable.description],
                            logoUrl = row[OrganizationsTable.logoUrl],
                            industry = row[OrganizationsTable.industry],
                            size = row[OrganizationsTable.size],
                            subscriptionTier = row[OrganizationsTable.subscriptionTier],
                            ssoEnabled = row[OrganizationsTable.ssoEnabled],
                            memberCount = memberCount.toInt(),
                            createdAt = Instant.ofEpochMilli(row[OrganizationsTable.createdAt]).toString(),
                            updatedAt = Instant.ofEpochMilli(row[OrganizationsTable.updatedAt]).toString()
                        )
                    }
            }
            
            call.respond(mapOf("organizations" to orgs))
        }
        
        // Get specific organization
        get("/{orgId}") {
            val userId = call.requireAuth() ?: return@get
            val orgId = call.parameters["orgId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Organization ID required"))
                return@get
            }
            
            val org = transaction {
                // Verify membership
                val isMember = OrganizationMembersTable.select { 
                    (OrganizationMembersTable.organizationId eq UUID.fromString(orgId)) and
                    (OrganizationMembersTable.userId eq UUID.fromString(userId)) and
                    (OrganizationMembersTable.status eq "ACTIVE")
                }.count() > 0
                
                if (!isMember) return@transaction null
                
                OrganizationsTable.select { OrganizationsTable.id eq UUID.fromString(orgId) }
                    .firstOrNull()
                    ?.let { row ->
                        val memberCount = OrganizationMembersTable
                            .select { (OrganizationMembersTable.organizationId eq UUID.fromString(orgId)) and (OrganizationMembersTable.status eq "ACTIVE") }
                            .count()
                        
                        OrganizationDto(
                            id = row[OrganizationsTable.id].toString(),
                            name = row[OrganizationsTable.name],
                            slug = row[OrganizationsTable.slug],
                            description = row[OrganizationsTable.description],
                            logoUrl = row[OrganizationsTable.logoUrl],
                            industry = row[OrganizationsTable.industry],
                            size = row[OrganizationsTable.size],
                            subscriptionTier = row[OrganizationsTable.subscriptionTier],
                            ssoEnabled = row[OrganizationsTable.ssoEnabled],
                            memberCount = memberCount.toInt(),
                            createdAt = Instant.ofEpochMilli(row[OrganizationsTable.createdAt]).toString(),
                            updatedAt = Instant.ofEpochMilli(row[OrganizationsTable.updatedAt]).toString()
                        )
                    }
            }
            
            if (org == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Organization not found"))
                return@get
            }
            
            call.respond(OrganizationResponse(organization = org))
        }
        
        // Update organization
        put("/{orgId}") {
            val userId = call.requireAuth() ?: return@put
            val orgId = call.parameters["orgId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Organization ID required"))
                return@put
            }
            val request = call.receive<UpdateOrganizationRequest>()
            
            val success = transaction {
                // Check if user is admin/owner
                val isAdmin = OrganizationMembersTable.select {
                    (OrganizationMembersTable.organizationId eq UUID.fromString(orgId)) and
                    (OrganizationMembersTable.userId eq UUID.fromString(userId)) and
                    (OrganizationMembersTable.role inList listOf("OWNER", "ADMIN")) and
                    (OrganizationMembersTable.status eq "ACTIVE")
                }.count() > 0
                
                if (!isAdmin) return@transaction false
                
                OrganizationsTable.update({ OrganizationsTable.id eq UUID.fromString(orgId) }) {
                    request.name?.let { name -> it[OrganizationsTable.name] = name }
                    request.description?.let { desc -> it[description] = desc }
                    request.logoUrl?.let { logo -> it[logoUrl] = logo }
                    request.industry?.let { ind -> it[industry] = ind }
                    request.size?.let { s -> it[size] = s }
                    it[updatedAt] = Instant.now().toEpochMilli()
                }
                true
            }
            
            if (!success) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not authorized to update organization"))
                return@put
            }
            
            call.respond(mapOf("message" to "Organization updated"))
        }
        
        // ===== Members =====
        
        // Get organization members
        get("/{orgId}/members") {
            val userId = call.requireAuth() ?: return@get
            val orgId = call.parameters["orgId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Organization ID required"))
                return@get
            }
            
            val members = transaction {
                // Verify membership
                val isMember = OrganizationMembersTable.select { 
                    (OrganizationMembersTable.organizationId eq UUID.fromString(orgId)) and
                    (OrganizationMembersTable.userId eq UUID.fromString(userId)) and
                    (OrganizationMembersTable.status eq "ACTIVE")
                }.count() > 0
                
                if (!isMember) return@transaction null
                
                OrganizationMembersTable.select { 
                    OrganizationMembersTable.organizationId eq UUID.fromString(orgId)
                }.map { row ->
                    MemberDto(
                        id = row[OrganizationMembersTable.id].toString(),
                        userId = row[OrganizationMembersTable.userId].toString(),
                        email = null, // Would join with users table
                        name = null,
                        role = row[OrganizationMembersTable.role],
                        status = row[OrganizationMembersTable.status],
                        joinedAt = row[OrganizationMembersTable.joinedAt]?.let { 
                            Instant.ofEpochMilli(it).toString() 
                        }
                    )
                }
            }
            
            if (members == null) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member of this organization"))
                return@get
            }
            
            call.respond(MembersListResponse(members = members))
        }
        
        // Invite member
        post("/{orgId}/members/invite") {
            val userId = call.requireAuth() ?: return@post
            val orgId = call.parameters["orgId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Organization ID required"))
                return@post
            }
            val request = call.receive<InviteMemberRequest>()
            
            val validRoles = listOf("MEMBER", "MANAGER", "ADMIN")
            if (request.role !in validRoles) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid role"))
                return@post
            }
            
            val invitation = transaction {
                // Check if user is admin/owner
                val isAdmin = OrganizationMembersTable.select {
                    (OrganizationMembersTable.organizationId eq UUID.fromString(orgId)) and
                    (OrganizationMembersTable.userId eq UUID.fromString(userId)) and
                    (OrganizationMembersTable.role inList listOf("OWNER", "ADMIN")) and
                    (OrganizationMembersTable.status eq "ACTIVE")
                }.count() > 0
                
                if (!isAdmin) return@transaction null
                
                // Check for existing invitation
                val existingInvite = OrganizationInvitationsTable.select {
                    (OrganizationInvitationsTable.organizationId eq UUID.fromString(orgId)) and
                    (OrganizationInvitationsTable.email eq request.email) and
                    (OrganizationInvitationsTable.status eq "PENDING")
                }.count() > 0
                
                if (existingInvite) return@transaction "EXISTS"
                
                val now = Instant.now().toEpochMilli()
                val expiresAt = now + (7 * 24 * 60 * 60 * 1000) // 7 days
                val token = UUID.randomUUID().toString()
                
                val inviteId = OrganizationInvitationsTable.insert {
                    it[organizationId] = UUID.fromString(orgId)
                    it[email] = request.email
                    it[role] = request.role
                    it[invitedBy] = UUID.fromString(userId)
                    it[OrganizationInvitationsTable.token] = token
                    it[OrganizationInvitationsTable.expiresAt] = expiresAt
                    it[createdAt] = now
                } get OrganizationInvitationsTable.id
                
                InvitationDto(
                    id = inviteId.toString(),
                    email = request.email,
                    role = request.role,
                    status = "PENDING",
                    expiresAt = Instant.ofEpochMilli(expiresAt).toString(),
                    createdAt = Instant.ofEpochMilli(now).toString()
                )
            }
            
            when (invitation) {
                null -> call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not authorized to invite members"))
                "EXISTS" -> call.respond(HttpStatusCode.Conflict, mapOf("error" to "Invitation already pending for this email"))
                is InvitationDto -> call.respond(HttpStatusCode.Created, InvitationResponse(invitation = invitation))
            }
        }
        
        // Update member role
        put("/{orgId}/members/{memberId}/role") {
            val userId = call.requireAuth() ?: return@put
            val orgId = call.parameters["orgId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Organization ID required"))
                return@put
            }
            val memberId = call.parameters["memberId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Member ID required"))
                return@put
            }
            val request = call.receive<UpdateMemberRoleRequest>()
            
            val validRoles = listOf("MEMBER", "MANAGER", "ADMIN")
            if (request.role !in validRoles) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid role. Cannot assign OWNER role."))
                return@put
            }
            
            val success = transaction {
                // Check if user is owner (only owner can change roles)
                val isOwner = OrganizationMembersTable.select {
                    (OrganizationMembersTable.organizationId eq UUID.fromString(orgId)) and
                    (OrganizationMembersTable.userId eq UUID.fromString(userId)) and
                    (OrganizationMembersTable.role eq "OWNER") and
                    (OrganizationMembersTable.status eq "ACTIVE")
                }.count() > 0
                
                if (!isOwner) return@transaction false
                
                // Cannot change owner's role
                val targetRole = OrganizationMembersTable.select {
                    OrganizationMembersTable.id eq UUID.fromString(memberId)
                }.firstOrNull()?.get(OrganizationMembersTable.role)
                
                if (targetRole == "OWNER") return@transaction false
                
                OrganizationMembersTable.update({ OrganizationMembersTable.id eq UUID.fromString(memberId) }) {
                    it[role] = request.role
                    it[updatedAt] = Instant.now().toEpochMilli()
                }
                true
            }
            
            if (!success) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not authorized to change member roles"))
                return@put
            }
            
            call.respond(mapOf("message" to "Member role updated"))
        }
        
        // Remove member
        delete("/{orgId}/members/{memberId}") {
            val userId = call.requireAuth() ?: return@delete
            val orgId = call.parameters["orgId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Organization ID required"))
                return@delete
            }
            val memberId = call.parameters["memberId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Member ID required"))
                return@delete
            }
            
            val success = transaction {
                // Check if user is admin/owner
                val isAdmin = OrganizationMembersTable.select {
                    (OrganizationMembersTable.organizationId eq UUID.fromString(orgId)) and
                    (OrganizationMembersTable.userId eq UUID.fromString(userId)) and
                    (OrganizationMembersTable.role inList listOf("OWNER", "ADMIN")) and
                    (OrganizationMembersTable.status eq "ACTIVE")
                }.count() > 0
                
                if (!isAdmin) return@transaction false
                
                // Cannot remove owner
                val targetRole = OrganizationMembersTable.select {
                    OrganizationMembersTable.id eq UUID.fromString(memberId)
                }.firstOrNull()?.get(OrganizationMembersTable.role)
                
                if (targetRole == "OWNER") return@transaction false
                
                OrganizationMembersTable.update({ OrganizationMembersTable.id eq UUID.fromString(memberId) }) {
                    it[status] = "REMOVED"
                    it[updatedAt] = Instant.now().toEpochMilli()
                }
                true
            }
            
            if (!success) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not authorized to remove members"))
                return@delete
            }
            
            call.respond(mapOf("message" to "Member removed"))
        }
        
        // ===== Templates =====
        
        // Get organization templates
        get("/{orgId}/templates") {
            val userId = call.requireAuth() ?: return@get
            val orgId = call.parameters["orgId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Organization ID required"))
                return@get
            }
            
            val templates = transaction {
                // Verify membership
                val isMember = OrganizationMembersTable.select { 
                    (OrganizationMembersTable.organizationId eq UUID.fromString(orgId)) and
                    (OrganizationMembersTable.userId eq UUID.fromString(userId)) and
                    (OrganizationMembersTable.status eq "ACTIVE")
                }.count() > 0
                
                if (!isMember) return@transaction null
                
                OrganizationTemplatesTable.select { 
                    OrganizationTemplatesTable.organizationId eq UUID.fromString(orgId)
                }.map { row ->
                    TemplateDto(
                        id = row[OrganizationTemplatesTable.id].toString(),
                        name = row[OrganizationTemplatesTable.name],
                        description = row[OrganizationTemplatesTable.description],
                        type = row[OrganizationTemplatesTable.type],
                        category = row[OrganizationTemplatesTable.category],
                        tags = row[OrganizationTemplatesTable.tags]?.split(",") ?: emptyList(),
                        isDefault = row[OrganizationTemplatesTable.isDefault],
                        createdAt = Instant.ofEpochMilli(row[OrganizationTemplatesTable.createdAt]).toString()
                    )
                }
            }
            
            if (templates == null) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a member of this organization"))
                return@get
            }
            
            call.respond(TemplatesListResponse(templates = templates))
        }
        
        // Create template
        post("/{orgId}/templates") {
            val userId = call.requireAuth() ?: return@post
            val orgId = call.parameters["orgId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Organization ID required"))
                return@post
            }
            val request = call.receive<CreateTemplateRequest>()
            
            val validTypes = listOf("RESUME", "COVER_LETTER", "EMAIL")
            if (request.type !in validTypes) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid template type"))
                return@post
            }
            
            val template = transaction {
                // Check if user is admin/owner/manager
                val canCreate = OrganizationMembersTable.select {
                    (OrganizationMembersTable.organizationId eq UUID.fromString(orgId)) and
                    (OrganizationMembersTable.userId eq UUID.fromString(userId)) and
                    (OrganizationMembersTable.role inList listOf("OWNER", "ADMIN", "MANAGER")) and
                    (OrganizationMembersTable.status eq "ACTIVE")
                }.count() > 0
                
                if (!canCreate) return@transaction null
                
                val now = Instant.now().toEpochMilli()
                
                val templateId = OrganizationTemplatesTable.insert {
                    it[organizationId] = UUID.fromString(orgId)
                    it[name] = request.name
                    it[description] = request.description
                    it[type] = request.type
                    it[content] = request.content
                    it[category] = request.category
                    it[tags] = request.tags.joinToString(",")
                    it[isDefault] = request.isDefault
                    it[createdBy] = UUID.fromString(userId)
                    it[createdAt] = now
                    it[updatedAt] = now
                } get OrganizationTemplatesTable.id
                
                TemplateDto(
                    id = templateId.toString(),
                    name = request.name,
                    description = request.description,
                    type = request.type,
                    category = request.category,
                    tags = request.tags,
                    isDefault = request.isDefault,
                    createdAt = Instant.ofEpochMilli(now).toString()
                )
            }
            
            if (template == null) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not authorized to create templates"))
                return@post
            }
            
            call.respond(HttpStatusCode.Created, TemplateResponse(template = template))
        }
        
        // ===== Analytics =====
        
        // Get organization analytics
        get("/{orgId}/analytics") {
            val userId = call.requireAuth() ?: return@get
            val orgId = call.parameters["orgId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Organization ID required"))
                return@get
            }
            
            val analytics = transaction {
                // Check if user is admin/owner/manager
                val canView = OrganizationMembersTable.select {
                    (OrganizationMembersTable.organizationId eq UUID.fromString(orgId)) and
                    (OrganizationMembersTable.userId eq UUID.fromString(userId)) and
                    (OrganizationMembersTable.role inList listOf("OWNER", "ADMIN", "MANAGER")) and
                    (OrganizationMembersTable.status eq "ACTIVE")
                }.count() > 0
                
                if (!canView) return@transaction null
                
                val memberCount = OrganizationMembersTable.select {
                    (OrganizationMembersTable.organizationId eq UUID.fromString(orgId)) and
                    (OrganizationMembersTable.status eq "ACTIVE")
                }.count().toInt()
                
                val now = Instant.now()
                val thirtyDaysAgo = now.minusSeconds(30 * 24 * 60 * 60)
                
                // Placeholder analytics - would be computed from actual application data
                AnalyticsDto(
                    totalMembers = memberCount,
                    activeMembers = memberCount, // Simplified
                    totalApplications = 0, // Would aggregate from job tracker
                    totalInterviews = 0,
                    totalOffers = 0,
                    successRate = 0.0,
                    periodStart = thirtyDaysAgo.toString(),
                    periodEnd = now.toString()
                )
            }
            
            if (analytics == null) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not authorized to view analytics"))
                return@get
            }
            
            call.respond(AnalyticsResponse(analytics = analytics))
        }
        
        // ===== Activity Log =====
        
        // Get activity log
        get("/{orgId}/activity") {
            val userId = call.requireAuth() ?: return@get
            val orgId = call.parameters["orgId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Organization ID required"))
                return@get
            }
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            
            val activities = transaction {
                // Check if user is admin/owner
                val isAdmin = OrganizationMembersTable.select {
                    (OrganizationMembersTable.organizationId eq UUID.fromString(orgId)) and
                    (OrganizationMembersTable.userId eq UUID.fromString(userId)) and
                    (OrganizationMembersTable.role inList listOf("OWNER", "ADMIN")) and
                    (OrganizationMembersTable.status eq "ACTIVE")
                }.count() > 0
                
                if (!isAdmin) return@transaction null
                
                OrganizationActivityLogTable.select {
                    OrganizationActivityLogTable.organizationId eq UUID.fromString(orgId)
                }
                    .orderBy(OrganizationActivityLogTable.timestamp, SortOrder.DESC)
                    .limit(limit)
                    .map { row ->
                        ActivityDto(
                            id = row[OrganizationActivityLogTable.id].toString(),
                            userId = row[OrganizationActivityLogTable.userId].toString(),
                            userName = null, // Would join with users table
                            action = row[OrganizationActivityLogTable.action],
                            resourceType = row[OrganizationActivityLogTable.resourceType],
                            details = row[OrganizationActivityLogTable.details],
                            timestamp = Instant.ofEpochMilli(row[OrganizationActivityLogTable.timestamp]).toString()
                        )
                    }
            }
            
            if (activities == null) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not authorized to view activity log"))
                return@get
            }
            
            call.respond(ActivityLogResponse(activities = activities))
        }
    }
    
    // Accept invitation (public endpoint with token)
    post("/invitations/{token}/accept") {
        val userId = call.requireAuth() ?: return@post
        val token = call.parameters["token"] ?: run {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invitation token required"))
            return@post
        }
        
        val result = transaction {
            val invitation = OrganizationInvitationsTable.select {
                (OrganizationInvitationsTable.token eq token) and
                (OrganizationInvitationsTable.status eq "PENDING")
            }.firstOrNull() ?: return@transaction "NOT_FOUND"
            
            val expiresAt = invitation[OrganizationInvitationsTable.expiresAt]
            if (Instant.now().toEpochMilli() > expiresAt) {
                OrganizationInvitationsTable.update({ OrganizationInvitationsTable.token eq token }) {
                    it[status] = "EXPIRED"
                }
                return@transaction "EXPIRED"
            }
            
            val orgId = invitation[OrganizationInvitationsTable.organizationId]
            val role = invitation[OrganizationInvitationsTable.role]
            val now = Instant.now().toEpochMilli()
            
            // Check if already a member
            val existingMember = OrganizationMembersTable.select {
                (OrganizationMembersTable.organizationId eq orgId) and
                (OrganizationMembersTable.userId eq UUID.fromString(userId))
            }.firstOrNull()
            
            if (existingMember != null) {
                // Reactivate if removed
                if (existingMember[OrganizationMembersTable.status] == "REMOVED") {
                    OrganizationMembersTable.update({
                        OrganizationMembersTable.id eq existingMember[OrganizationMembersTable.id]
                    }) {
                        it[status] = "ACTIVE"
                        it[OrganizationMembersTable.role] = role
                        it[joinedAt] = now
                        it[updatedAt] = now
                    }
                } else {
                    return@transaction "ALREADY_MEMBER"
                }
            } else {
                OrganizationMembersTable.insert {
                    it[organizationId] = orgId
                    it[OrganizationMembersTable.userId] = UUID.fromString(userId)
                    it[OrganizationMembersTable.role] = role
                    it[status] = "ACTIVE"
                    it[joinedAt] = now
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
            
            // Mark invitation as accepted
            OrganizationInvitationsTable.update({ OrganizationInvitationsTable.token eq token }) {
                it[status] = "ACCEPTED"
            }
            
            "SUCCESS"
        }
        
        when (result) {
            "NOT_FOUND" -> call.respond(HttpStatusCode.NotFound, mapOf("error" to "Invitation not found"))
            "EXPIRED" -> call.respond(HttpStatusCode.Gone, mapOf("error" to "Invitation has expired"))
            "ALREADY_MEMBER" -> call.respond(HttpStatusCode.Conflict, mapOf("error" to "Already a member of this organization"))
            "SUCCESS" -> call.respond(mapOf("message" to "Successfully joined organization"))
        }
    }
}
