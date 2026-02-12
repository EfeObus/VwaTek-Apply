package com.vwatek.apply.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * PIPEDA Compliance Database Tables
 * 
 * Supports:
 * - Consent tracking and audit trail
 * - Data access requests
 * - Data export/deletion requests
 */

/**
 * User consent records with full audit trail
 */
object ConsentRecordsTable : Table("consent_records") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val purpose = varchar("purpose", 50) // ESSENTIAL, ANALYTICS, PERSONALIZATION, MARKETING, THIRD_PARTY_SHARING
    val status = varchar("status", 20) // GRANTED, DENIED, WITHDRAWN
    val policyVersion = varchar("policy_version", 20)
    val grantedAt = timestamp("granted_at").nullable()
    val withdrawnAt = timestamp("withdrawn_at").nullable()
    val ipAddress = varchar("ip_address", 45).nullable() // IPv6 compatible
    val userAgent = text("user_agent").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(false, userId)
        index(false, userId, purpose)
    }
}

/**
 * Consent audit log for compliance
 * Immutable log of all consent changes
 */
object ConsentAuditLogTable : Table("consent_audit_log") {
    val id = long("id").autoIncrement()
    val userId = varchar("user_id", 36)
    val purpose = varchar("purpose", 50)
    val action = varchar("action", 20) // GRANT, WITHDRAW, DENY
    val previousStatus = varchar("previous_status", 20).nullable()
    val newStatus = varchar("new_status", 20)
    val policyVersion = varchar("policy_version", 20)
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()
    val timestamp = timestamp("timestamp")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(false, userId)
        index(false, timestamp)
    }
}

/**
 * Data subject access requests (DSAR)
 * For PIPEDA right to access and portability
 */
object DataAccessRequestsTable : Table("data_access_requests") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val requestType = varchar("request_type", 30) // ACCESS, EXPORT, DELETION, CORRECTION
    val status = varchar("status", 20) // PENDING, PROCESSING, COMPLETED, REJECTED
    val reason = text("reason").nullable() // Reason for request
    val notes = text("notes").nullable() // Admin notes
    val dataUrl = text("data_url").nullable() // URL to download exported data
    val dataExpiresAt = timestamp("data_expires_at").nullable()
    val requestedAt = timestamp("requested_at")
    val processedAt = timestamp("processed_at").nullable()
    val processedBy = varchar("processed_by", 36).nullable() // Admin user ID
    val completedAt = timestamp("completed_at").nullable()
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(false, userId)
        index(false, status)
        index(false, requestedAt)
    }
}

/**
 * Data retention policy tracking
 * Tracks when data should be deleted based on retention policy
 */
object DataRetentionTable : Table("data_retention") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val dataType = varchar("data_type", 50) // RESUME, COVER_LETTER, INTERVIEW, ANALYTICS, etc.
    val entityId = varchar("entity_id", 36)
    val retentionPolicy = varchar("retention_policy", 50)
    val expiresAt = timestamp("expires_at")
    val deletedAt = timestamp("deleted_at").nullable()
    val deletionReason = varchar("deletion_reason", 50).nullable() // POLICY, USER_REQUEST, CONSENT_WITHDRAWN
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(false, userId)
        index(false, expiresAt)
        index(false, dataType, expiresAt)
    }
}

/**
 * Third-party data sharing log
 * Records when and what data was shared with third parties
 */
object DataSharingLogTable : Table("data_sharing_log") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val thirdParty = varchar("third_party", 100) // e.g., "Google AI", "Job Bank Canada"
    val purpose = varchar("purpose", 50)
    val dataTypes = text("data_types") // JSON array of data types shared
    val consentRecordId = varchar("consent_record_id", 36).nullable()
    val sharedAt = timestamp("shared_at")
    val responseReceived = bool("response_received").default(false)
    val requestDetails = text("request_details").nullable() // Anonymized request info
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(false, userId)
        index(false, thirdParty)
        index(false, sharedAt)
    }
}
