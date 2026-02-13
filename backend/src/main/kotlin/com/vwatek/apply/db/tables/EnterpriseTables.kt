package com.vwatek.apply.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Phase 5: Enterprise Tables for VwaTek Apply
 * Supports organizations, teams, SSO, templates, and admin features
 */

// Organizations Table
object OrganizationsTable : Table("organizations") {
    val id = varchar("id", 36)
    val name = varchar("name", 255)
    val slug = varchar("slug", 100).uniqueIndex()
    val description = text("description").nullable()
    val logoUrl = text("logo_url").nullable()
    val industry = varchar("industry", 100).nullable()
    val size = varchar("size", 20).nullable()              // SMALL, MEDIUM, LARGE, ENTERPRISE
    val settingsJson = text("settings_json")               // OrganizationSettings as JSON
    val subscriptionTier = varchar("subscription_tier", 20).default("FREE")
    val ssoEnabled = bool("sso_enabled").default(false)
    val ssoProvider = varchar("sso_provider", 30).nullable()   // SAML, OIDC, AZURE_AD, etc.
    val ssoConfigJson = text("sso_config_json").nullable()     // SSOConfig as JSON (encrypted)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index("idx_organizations_name", false, name)
    }
}

// Organization Members Table
object OrganizationMembersTable : Table("organization_members") {
    val id = varchar("id", 36)
    val organizationId = varchar("organization_id", 36).references(OrganizationsTable.id, onDelete = ReferenceOption.CASCADE)
    val userId = varchar("user_id", 36).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val role = varchar("role", 20)                 // OWNER, ADMIN, MANAGER, MEMBER
    val status = varchar("status", 20)             // PENDING, ACTIVE, SUSPENDED, REMOVED
    val invitedBy = varchar("invited_by", 36).references(UsersTable.id).nullable()
    val invitedAt = timestamp("invited_at").nullable()
    val joinedAt = timestamp("joined_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        uniqueIndex("idx_org_member_unique", organizationId, userId)
        index("idx_org_members_org", false, organizationId)
        index("idx_org_members_user", false, userId)
    }
}

// Organization Invitations Table
object OrganizationInvitationsTable : Table("organization_invitations") {
    val id = varchar("id", 36)
    val organizationId = varchar("organization_id", 36).references(OrganizationsTable.id, onDelete = ReferenceOption.CASCADE)
    val email = varchar("email", 255)
    val role = varchar("role", 20)
    val invitedBy = varchar("invited_by", 36).references(UsersTable.id)
    val token = varchar("token", 64).uniqueIndex()
    val status = varchar("status", 20)             // PENDING, ACCEPTED, DECLINED, EXPIRED, REVOKED
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index("idx_invitations_org", false, organizationId)
        index("idx_invitations_email", false, email)
    }
}

// Organization Templates Table
object OrganizationTemplatesTable : Table("organization_templates") {
    val id = varchar("id", 36)
    val organizationId = varchar("organization_id", 36).references(OrganizationsTable.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val type = varchar("type", 20)                 // RESUME, COVER_LETTER, EMAIL
    val content = text("content")
    val category = varchar("category", 100).nullable()
    val tags = text("tags").nullable()             // JSON array
    val isDefault = bool("is_default").default(false)
    val createdBy = varchar("created_by", 36).references(UsersTable.id)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index("idx_templates_org", false, organizationId)
        index("idx_templates_type", false, type)
    }
}

// Organization Activity Log Table
object OrganizationActivityLogTable : Table("organization_activity_log") {
    val id = varchar("id", 36)
    val organizationId = varchar("organization_id", 36).references(OrganizationsTable.id, onDelete = ReferenceOption.CASCADE)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val action = varchar("action", 50)             // MEMBER_JOINED, TEMPLATE_CREATED, etc.
    val resourceType = varchar("resource_type", 50)
    val resourceId = varchar("resource_id", 36).nullable()
    val details = text("details").nullable()
    val ipAddress = varchar("ip_address", 45).nullable()
    val timestamp = timestamp("timestamp")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index("idx_activity_org", false, organizationId)
        index("idx_activity_timestamp", false, timestamp)
    }
}

// LinkedIn Profiles Table
object LinkedInProfilesTable : Table("linkedin_profiles") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val linkedInUrl = text("linkedin_url").nullable()
    val headline = varchar("headline", 500).nullable()
    val summary = text("summary").nullable()
    val currentPositionJson = text("current_position_json").nullable()
    val positionsJson = text("positions_json")         // JSON array
    val educationJson = text("education_json")         // JSON array
    val skillsJson = text("skills_json")               // JSON array
    val endorsementsJson = text("endorsements_json").nullable()  // JSON map
    val recommendations = integer("recommendations").default(0)
    val connections = integer("connections").default(0)
    val profilePhotoUrl = text("profile_photo_url").nullable()
    val bannerPhotoUrl = text("banner_photo_url").nullable()
    val industry = varchar("industry", 100).nullable()
    val location = varchar("location", 255).nullable()
    val importedAt = timestamp("imported_at").nullable()
    val lastAnalyzedAt = timestamp("last_analyzed_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        uniqueIndex("idx_linkedin_user", userId)
    }
}

// LinkedIn Analysis History Table
object LinkedInAnalysisHistoryTable : Table("linkedin_analysis_history") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val profileId = varchar("profile_id", 36).references(LinkedInProfilesTable.id, onDelete = ReferenceOption.CASCADE)
    val analysisJson = text("analysis_json")           // LinkedInAnalysis as JSON
    val appliedRecommendationsJson = text("applied_recommendations_json").nullable()  // JSON array
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index("idx_linkedin_analysis_user", false, userId)
    }
}

// SSO Sessions Table
object SSOSessionsTable : Table("sso_sessions") {
    val id = varchar("id", 36)
    val organizationId = varchar("organization_id", 36).references(OrganizationsTable.id, onDelete = ReferenceOption.CASCADE)
    val userId = varchar("user_id", 36).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val sessionIndex = varchar("session_index", 255).nullable()  // SAML session index
    val nameId = varchar("name_id", 255)           // SAML NameID or OIDC sub
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index("idx_sso_sessions_org", false, organizationId)
        index("idx_sso_sessions_user", false, userId)
    }
}

// Admin Reports Table (for generating scheduled reports)
object AdminReportsTable : Table("admin_reports") {
    val id = varchar("id", 36)
    val organizationId = varchar("organization_id", 36).references(OrganizationsTable.id, onDelete = ReferenceOption.CASCADE)
    val reportType = varchar("report_type", 50)    // USAGE, ACTIVITY, MEMBERS
    val periodStart = timestamp("period_start")
    val periodEnd = timestamp("period_end")
    val dataJson = text("data_json")               // Report data as JSON
    val createdBy = varchar("created_by", 36).references(UsersTable.id).nullable()
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index("idx_reports_org", false, organizationId)
    }
}

// Organization Subscription History (for enterprise billing)
object OrganizationSubscriptionHistoryTable : Table("organization_subscription_history") {
    val id = varchar("id", 36)
    val organizationId = varchar("organization_id", 36).references(OrganizationsTable.id, onDelete = ReferenceOption.CASCADE)
    val previousTier = varchar("previous_tier", 20)
    val newTier = varchar("new_tier", 20)
    val seats = integer("seats")
    val changedBy = varchar("changed_by", 36).references(UsersTable.id)
    val changeReason = text("change_reason").nullable()
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index("idx_org_sub_history", false, organizationId)
    }
}
