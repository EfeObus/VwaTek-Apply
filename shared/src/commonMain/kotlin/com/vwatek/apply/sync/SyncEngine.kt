package com.vwatek.apply.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Cross-platform Sync Engine interface
 * 
 * Handles synchronization of user data across devices with:
 * - Automatic background sync
 * - Offline operation queuing
 * - Conflict resolution
 * - Network-aware sync scheduling
 */
interface SyncEngine {
    
    /**
     * Current sync state as a reactive flow
     */
    val syncState: StateFlow<SyncState>
    
    /**
     * Register this device with the sync service
     * @return Device registration result
     */
    suspend fun registerDevice(request: DeviceRegistrationRequest): Result<DeviceRegistrationResponse>
    
    /**
     * Perform a full sync
     * Downloads all user data from server
     */
    suspend fun performFullSync(): Result<SyncResponse>
    
    /**
     * Perform an incremental sync
     * Only syncs changes since last sync
     */
    suspend fun performIncrementalSync(): Result<SyncResponse>
    
    /**
     * Queue an operation to be synced when online
     */
    suspend fun queueOperation(operation: OfflineOperation)
    
    /**
     * Get all pending offline operations
     */
    suspend fun getPendingOperations(): List<OfflineOperation>
    
    /**
     * Clear pending operations after successful sync
     */
    suspend fun clearPendingOperations(operationIds: List<String>)
    
    /**
     * Check if there are pending operations
     */
    suspend fun hasPendingOperations(): Boolean
    
    /**
     * Enable or disable automatic sync
     */
    fun setAutoSyncEnabled(enabled: Boolean)
    
    /**
     * Force sync now (if network available)
     */
    suspend fun syncNow(): Result<SyncResponse>
    
    /**
     * Cancel any ongoing sync operation
     */
    fun cancelSync()
}

/**
 * Sync engine factory - creates platform-specific implementations
 */
expect class SyncEngineFactory {
    fun create(
        apiBaseUrl: String,
        authToken: () -> String?,
        onSyncComplete: ((SyncResponse) -> Unit)? = null,
        onSyncError: ((Throwable) -> Unit)? = null
    ): SyncEngine
}

/**
 * Local storage interface for sync data persistence
 */
interface SyncStorage {
    /**
     * Save pending operation
     */
    suspend fun saveOperation(operation: OfflineOperation)
    
    /**
     * Get all pending operations
     */
    suspend fun getAllPendingOperations(): List<OfflineOperation>
    
    /**
     * Delete operations by IDs
     */
    suspend fun deleteOperations(ids: List<String>)
    
    /**
     * Update operation status
     */
    suspend fun updateOperationStatus(id: String, status: SyncStatus, errorMessage: String? = null)
    
    /**
     * Get last sync timestamp
     */
    suspend fun getLastSyncTimestamp(): Long
    
    /**
     * Save last sync timestamp
     */
    suspend fun setLastSyncTimestamp(timestamp: Long)
    
    /**
     * Get device ID
     */
    suspend fun getDeviceId(): String?
    
    /**
     * Save device ID
     */
    suspend fun setDeviceId(deviceId: String)
    
    /**
     * Clear all sync data (for logout)
     */
    suspend fun clearAllSyncData()
}

/**
 * Helper extension to create an offline operation for an entity
 */
inline fun <reified T> createSyncOperation(
    entityType: SyncEntityType,
    entityId: String,
    operationType: SyncOperationType,
    entity: T?,
    serializer: (T) -> String
): OfflineOperation {
    return OfflineOperation(
        id = generateSyncOperationId(),
        entityType = entityType,
        entityId = entityId,
        operationType = operationType,
        payload = entity?.let { serializer(it) } ?: "",
        timestamp = currentTimeMillis()
    )
}

/**
 * Platform-specific ID generation
 */
expect fun generateSyncOperationId(): String

/**
 * Platform-specific current time
 */
expect fun currentTimeMillis(): Long
