package com.vwatek.apply.sync

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Sync Engine for VwaTek Apply
 * 
 * Implements a robust synchronization system with:
 * - Conflict-free replicated data types (CRDT) concepts
 * - Last-write-wins conflict resolution
 * - Offline operation queuing
 * - Incremental sync support
 */

/**
 * Represents a syncable entity type
 */
enum class SyncEntityType {
    RESUME,
    COVER_LETTER,
    INTERVIEW_SESSION,
    SETTINGS,
    USER_PROFILE
}

/**
 * Represents the type of sync operation
 */
enum class SyncOperationType {
    CREATE,
    UPDATE,
    DELETE
}

/**
 * Status of a sync operation
 */
enum class SyncStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CONFLICT
}

/**
 * Represents a pending offline operation
 */
@Serializable
data class OfflineOperation(
    val id: String,
    val entityType: SyncEntityType,
    val entityId: String,
    val operationType: SyncOperationType,
    val payload: String, // JSON serialized entity data
    val timestamp: Long,
    val retryCount: Int = 0,
    val status: SyncStatus = SyncStatus.PENDING
)

/**
 * Metadata for a syncable entity
 */
@Serializable
data class SyncMetadata(
    val entityType: SyncEntityType,
    val entityId: String,
    val version: Long,
    val lastModifiedAt: Long,
    val lastModifiedBy: String?, // Device ID
    val isDeleted: Boolean = false,
    val checksum: String? = null
)

/**
 * Represents a sync request from client to server
 */
@Serializable
data class SyncRequest(
    val deviceId: String,
    val lastSyncTimestamp: Long,
    val operations: List<OfflineOperation>,
    val entityTypes: List<SyncEntityType>? = null // Optional filter for specific entity types
)

/**
 * Represents a sync response from server to client
 */
@Serializable
data class SyncResponse(
    val success: Boolean,
    val serverTimestamp: Long,
    val changes: List<SyncChange>,
    val conflicts: List<SyncConflict>,
    val errors: List<SyncError>
)

/**
 * Represents a change that needs to be applied on the client
 */
@Serializable
data class SyncChange(
    val entityType: SyncEntityType,
    val entityId: String,
    val operationType: SyncOperationType,
    val data: String?, // JSON serialized entity data (null for deletes)
    val metadata: SyncMetadata
)

/**
 * Represents a sync conflict
 */
@Serializable
data class SyncConflict(
    val entityType: SyncEntityType,
    val entityId: String,
    val localVersion: Long,
    val serverVersion: Long,
    val localData: String,
    val serverData: String,
    val resolution: ConflictResolution,
    val resolvedData: String?
)

/**
 * How a conflict was resolved
 */
@Serializable
enum class ConflictResolution {
    SERVER_WINS,
    CLIENT_WINS,
    MERGED,
    MANUAL_REQUIRED
}

/**
 * Represents a sync error
 */
@Serializable
data class SyncError(
    val operationId: String,
    val entityType: SyncEntityType,
    val entityId: String,
    val errorCode: String,
    val errorMessage: String
)

/**
 * Device registration request
 */
@Serializable
data class DeviceRegistrationRequest(
    val deviceId: String?,
    val deviceName: String,
    val deviceType: String, // ANDROID, IOS, WEB
    val deviceModel: String?,
    val osVersion: String?,
    val appVersion: String,
    val pushToken: String?
)

/**
 * Device registration response
 */
@Serializable
data class DeviceRegistrationResponse(
    val deviceId: String,
    val isNewDevice: Boolean,
    val lastSyncTimestamp: Long?
)

/**
 * Sync state for tracking local sync progress
 */
@Serializable
data class SyncState(
    val deviceId: String,
    val lastSyncTimestamp: Long,
    val pendingOperationsCount: Int,
    val isSyncing: Boolean,
    val lastSyncError: String?
)
