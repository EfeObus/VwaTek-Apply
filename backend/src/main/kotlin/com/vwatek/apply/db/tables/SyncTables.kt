package com.vwatek.apply.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Sync-related database tables for VwaTek Apply
 * 
 * These tables support:
 * - Device registration and management
 * - Conflict-free sync operations using last-write-wins strategy
 * - Offline operation queuing
 * - Sync state tracking per device
 */

/**
 * Registered devices for a user
 * Tracks all devices that have synced with the user's account
 */
object DevicesTable : Table("devices") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val deviceName = varchar("device_name", 255)
    val deviceType = varchar("device_type", 50) // ANDROID, IOS, WEB
    val deviceModel = varchar("device_model", 255).nullable()
    val osVersion = varchar("os_version", 50).nullable()
    val appVersion = varchar("app_version", 50)
    val pushToken = text("push_token").nullable()
    val lastSyncAt = timestamp("last_sync_at").nullable()
    val lastActiveAt = timestamp("last_active_at")
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(false, userId)
        index(false, userId, isActive)
    }
}

/**
 * Sync log table for tracking all sync operations
 * Used for debugging and analytics
 */
object SyncLogsTable : Table("sync_logs") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val deviceId = varchar("device_id", 36).references(DevicesTable.id)
    val syncType = varchar("sync_type", 50) // FULL, INCREMENTAL, PUSH, PULL
    val status = varchar("status", 20) // STARTED, COMPLETED, FAILED
    val itemsPushed = integer("items_pushed").default(0)
    val itemsPulled = integer("items_pulled").default(0)
    val conflictsResolved = integer("conflicts_resolved").default(0)
    val errorMessage = text("error_message").nullable()
    val durationMs = long("duration_ms").nullable()
    val startedAt = timestamp("started_at")
    val completedAt = timestamp("completed_at").nullable()
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(false, userId)
        index(false, deviceId)
        index(false, startedAt)
    }
}

/**
 * Per-entity sync metadata
 * Tracks the sync state for each syncable entity (resumes, cover letters, etc.)
 */
object SyncMetadataTable : Table("sync_metadata") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val entityType = varchar("entity_type", 50) // RESUME, COVER_LETTER, INTERVIEW_SESSION, SETTINGS
    val entityId = varchar("entity_id", 36)
    val version = long("version").default(1)
    val lastModifiedAt = timestamp("last_modified_at")
    val lastModifiedBy = varchar("last_modified_by", 36).nullable() // Device ID that made the change
    val isDeleted = bool("is_deleted").default(false)
    val deletedAt = timestamp("deleted_at").nullable()
    val checksum = varchar("checksum", 64).nullable() // SHA-256 hash for conflict detection
    val createdAt = timestamp("created_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        uniqueIndex("idx_sync_entity", userId, entityType, entityId)
        index(false, userId, entityType, lastModifiedAt)
        index(false, lastModifiedAt)
    }
}

/**
 * Device sync state
 * Tracks what each device has synced to determine what to send on next sync
 */
object DeviceSyncStateTable : Table("device_sync_state") {
    val id = varchar("id", 36)
    val deviceId = varchar("device_id", 36).references(DevicesTable.id)
    val entityType = varchar("entity_type", 50)
    val lastSyncedVersion = long("last_synced_version").default(0)
    val lastSyncedAt = timestamp("last_synced_at")
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        uniqueIndex("idx_device_entity_sync", deviceId, entityType)
    }
}

/**
 * Offline operation queue
 * Stores operations made while offline for later sync
 */
object OfflineOperationsTable : Table("offline_operations") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val deviceId = varchar("device_id", 36).references(DevicesTable.id)
    val operationType = varchar("operation_type", 20) // CREATE, UPDATE, DELETE
    val entityType = varchar("entity_type", 50)
    val entityId = varchar("entity_id", 36)
    val payload = text("payload") // JSON payload of the operation
    val status = varchar("status", 20).default("PENDING") // PENDING, PROCESSING, COMPLETED, FAILED
    val retryCount = integer("retry_count").default(0)
    val errorMessage = text("error_message").nullable()
    val createdAt = timestamp("created_at")
    val processedAt = timestamp("processed_at").nullable()
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(false, userId, status)
        index(false, deviceId, status)
        index(false, createdAt)
    }
}

/**
 * Sync conflicts table
 * Records conflicts that occurred during sync for audit and potential manual resolution
 */
object SyncConflictsTable : Table("sync_conflicts") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val syncLogId = varchar("sync_log_id", 36).references(SyncLogsTable.id)
    val entityType = varchar("entity_type", 50)
    val entityId = varchar("entity_id", 36)
    val localVersion = long("local_version")
    val serverVersion = long("server_version")
    val localData = text("local_data") // JSON snapshot of local state
    val serverData = text("server_data") // JSON snapshot of server state
    val resolution = varchar("resolution", 20) // LOCAL_WINS, SERVER_WINS, MERGED, MANUAL
    val resolvedData = text("resolved_data").nullable() // Final resolved state
    val createdAt = timestamp("created_at")
    val resolvedAt = timestamp("resolved_at").nullable()
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(false, userId)
        index(false, syncLogId)
        index(false, entityType, entityId)
    }
}

/**
 * User data region table for Canadian data residency compliance
 * Tracks which region a user's data is stored in
 */
object UserDataRegionsTable : Table("user_data_regions") {
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val region = varchar("region", 50) // northamerica-northeast1 (Montreal), etc.
    val isPrimary = bool("is_primary").default(true)
    val migratedAt = timestamp("migrated_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    
    override val primaryKey = PrimaryKey(userId, region)
    
    init {
        index(false, region)
    }
}

/**
 * Change data capture table for real-time sync notifications
 * Populated by database triggers when entities change
 */
object ChangeFeedTable : Table("change_feed") {
    val id = long("id").autoIncrement()
    val userId = varchar("user_id", 36)
    val entityType = varchar("entity_type", 50)
    val entityId = varchar("entity_id", 36)
    val changeType = varchar("change_type", 20) // INSERT, UPDATE, DELETE
    val changedAt = timestamp("changed_at")
    val processed = bool("processed").default(false)
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        index(false, userId, processed, changedAt)
        index(false, changedAt)
    }
}
