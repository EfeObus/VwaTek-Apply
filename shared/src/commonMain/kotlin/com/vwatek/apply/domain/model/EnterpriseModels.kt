package com.vwatek.apply.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Enterprise and Organization Models for VwaTek Apply
 * Supports team/organization features for enterprise customers
 */

/**
 * Organization (Team/Company)
 */
@Serializable
data class Organization(
    val id: String,
    val name: String,
    val slug: String,                    // URL-friendly identifier
    val description: String? = null,
    val logoUrl: String? = null,
    val industry: String? = null,
    val size: OrganizationSize? = null,
    val settings: OrganizationSettings,
    val subscriptionTier: SubscriptionTier,
    val ssoEnabled: Boolean = false,
    val ssoProvider: SSOProvider? = null,
    val ssoConfig: SSOConfig? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Organization size categories
 */
@Serializable
enum class OrganizationSize {
    SMALL,       // 1-50 employees
    MEDIUM,      // 51-200 employees
    LARGE,       // 201-1000 employees
    ENTERPRISE   // 1000+ employees
}

/**
 * Organization-level settings
 */
@Serializable
data class OrganizationSettings(
    val allowMemberInvites: Boolean = true,
    val requireApprovalForJoin: Boolean = true,
    val defaultMemberRole: OrganizationRole = OrganizationRole.MEMBER,
    val shareTemplatesWithMembers: Boolean = true,
    val allowExternalSharing: Boolean = false,
    val enforcePasswordPolicy: Boolean = false,
    val requireMfa: Boolean = false,
    val brandingEnabled: Boolean = false,
    val customBranding: CustomBranding? = null
)

/**
 * Custom branding for white-label support
 */
@Serializable
data class CustomBranding(
    val primaryColor: String? = null,
    val secondaryColor: String? = null,
    val logoUrl: String? = null,
    val faviconUrl: String? = null,
    val companyName: String? = null,
    val customDomain: String? = null
)

/**
 * SSO providers
 */
@Serializable
enum class SSOProvider {
    SAML,
    OIDC,
    AZURE_AD,
    OKTA,
    GOOGLE_WORKSPACE
}

/**
 * SSO configuration
 */
@Serializable
data class SSOConfig(
    val provider: SSOProvider,
    val entityId: String? = null,
    val ssoUrl: String? = null,
    val certificate: String? = null,     // X.509 certificate for SAML
    val clientId: String? = null,        // For OIDC
    val clientSecret: String? = null,    // Encrypted
    val issuer: String? = null,
    val authorizationEndpoint: String? = null,
    val tokenEndpoint: String? = null,
    val userInfoEndpoint: String? = null,
    val domainHint: String? = null
)

/**
 * Organization member
 */
@Serializable
data class OrganizationMember(
    val id: String,
    val organizationId: String,
    val userId: String,
    val user: User? = null,              // Loaded when needed
    val role: OrganizationRole,
    val status: MemberStatus,
    val invitedBy: String? = null,
    val invitedAt: Instant? = null,
    val joinedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Member roles within an organization
 */
@Serializable
enum class OrganizationRole {
    OWNER,      // Full access, can delete organization
    ADMIN,      // Can manage members and settings
    MANAGER,    // Can view team analytics, manage templates
    MEMBER      // Basic member access
}

/**
 * Member invitation/join status
 */
@Serializable
enum class MemberStatus {
    PENDING,    // Invitation sent
    ACTIVE,     // Member is active
    SUSPENDED,  // Temporarily suspended
    REMOVED     // Removed from organization
}

/**
 * Organization invitation
 */
@Serializable
data class OrganizationInvitation(
    val id: String,
    val organizationId: String,
    val email: String,
    val role: OrganizationRole,
    val invitedBy: String,
    val token: String,           // Unique invitation token
    val status: InvitationStatus,
    val expiresAt: Instant,
    val createdAt: Instant
)

/**
 * Invitation status
 */
@Serializable
enum class InvitationStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    EXPIRED,
    REVOKED
}

/**
 * Organization template (shared resume/cover letter templates)
 */
@Serializable
data class OrganizationTemplate(
    val id: String,
    val organizationId: String,
    val name: String,
    val description: String? = null,
    val type: TemplateType,
    val content: String,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val isDefault: Boolean = false,
    val createdBy: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Template types
 */
@Serializable
enum class TemplateType {
    RESUME,
    COVER_LETTER,
    EMAIL
}

/**
 * Organization analytics summary
 */
@Serializable
data class OrganizationAnalytics(
    val organizationId: String,
    val period: AnalyticsPeriod,
    val totalMembers: Int,
    val activeMembers: Int,
    val totalApplications: Int,
    val totalInterviews: Int,
    val totalOffers: Int,
    val averageTimeToOffer: Double?,    // Days
    val successRate: Double,             // Percentage of offers vs applications
    val topSkills: List<SkillFrequency>,
    val memberActivity: List<MemberActivity>,
    val createdAt: Instant
)

/**
 * Analytics time period
 */
@Serializable
data class AnalyticsPeriod(
    val startDate: Instant,
    val endDate: Instant,
    val periodType: PeriodType
)

/**
 * Period type for analytics
 */
@Serializable
enum class PeriodType {
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY,
    CUSTOM
}

/**
 * Skill frequency in organization
 */
@Serializable
data class SkillFrequency(
    val skill: String,
    val count: Int,
    val percentage: Double
)

/**
 * Member activity summary
 */
@Serializable
data class MemberActivity(
    val userId: String,
    val userName: String,
    val applicationsCreated: Int,
    val resumesUpdated: Int,
    val interviewsScheduled: Int,
    val lastActiveAt: Instant?
)

/**
 * Create organization request
 */
@Serializable
data class CreateOrganizationRequest(
    val name: String,
    val description: String? = null,
    val industry: String? = null,
    val size: OrganizationSize? = null
)

/**
 * Update organization request
 */
@Serializable
data class UpdateOrganizationRequest(
    val name: String? = null,
    val description: String? = null,
    val logoUrl: String? = null,
    val industry: String? = null,
    val size: OrganizationSize? = null,
    val settings: OrganizationSettings? = null
)

/**
 * Invite member request
 */
@Serializable
data class InviteMemberRequest(
    val email: String,
    val role: OrganizationRole = OrganizationRole.MEMBER
)

/**
 * Bulk invite request
 */
@Serializable
data class BulkInviteRequest(
    val emails: List<String>,
    val role: OrganizationRole = OrganizationRole.MEMBER
)

/**
 * Update member role request
 */
@Serializable
data class UpdateMemberRoleRequest(
    val role: OrganizationRole
)

/**
 * Create template request
 */
@Serializable
data class CreateTemplateRequest(
    val name: String,
    val description: String? = null,
    val type: TemplateType,
    val content: String,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val isDefault: Boolean = false
)

/**
 * Admin dashboard statistics
 */
@Serializable
data class AdminDashboardStats(
    val organization: Organization,
    val memberCount: Int,
    val pendingInvitations: Int,
    val templateCount: Int,
    val analytics: OrganizationAnalytics?,
    val recentActivity: List<ActivityLogEntry>,
    val subscriptionUsage: SubscriptionUsage
)

/**
 * Activity log entry for admin dashboard
 */
@Serializable
data class ActivityLogEntry(
    val id: String,
    val userId: String,
    val userName: String,
    val action: ActivityAction,
    val resourceType: String,
    val resourceId: String? = null,
    val details: String? = null,
    val ipAddress: String? = null,
    val timestamp: Instant
)

/**
 * Activity action types
 */
@Serializable
enum class ActivityAction {
    MEMBER_JOINED,
    MEMBER_INVITED,
    MEMBER_REMOVED,
    ROLE_CHANGED,
    TEMPLATE_CREATED,
    TEMPLATE_UPDATED,
    TEMPLATE_DELETED,
    SETTINGS_UPDATED,
    SSO_CONFIGURED,
    BRANDING_UPDATED
}

/**
 * Subscription usage for organization
 */
@Serializable
data class SubscriptionUsage(
    val maxMembers: Int,
    val currentMembers: Int,
    val maxTemplates: Int,
    val currentTemplates: Int,
    val ssoEnabled: Boolean,
    val whitelabelEnabled: Boolean,
    val analyticsEnabled: Boolean
)
