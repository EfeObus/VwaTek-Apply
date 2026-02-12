package com.vwatek.apply.data.api

import com.vwatek.apply.sync.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

/**
 * Sync API Client for VwaTek Apply
 * 
 * Handles communication with the /api/sync endpoints
 * Uses DTOs that exactly match the backend format
 */
class SyncApiClient(
    private val httpClient: HttpClient,
    private val getAuthToken: () -> String?
) {
    
    // ========== API DTO Models (match backend exactly) ==========
    
    @Serializable
    data class ApiDeviceRegistrationRequest(
        val deviceId: String? = null,
        val deviceName: String,
        val deviceType: String,
        val deviceModel: String? = null,
        val osVersion: String? = null,
        val appVersion: String,
        val pushToken: String? = null
    )
    
    @Serializable
    data class ApiDeviceRegistrationResponse(
        val deviceId: String,
        val isNewDevice: Boolean,
        val lastSyncTimestamp: Long?
    )
    
    @Serializable
    data class ApiSyncRequest(
        val deviceId: String,
        val lastSyncTimestamp: Long,
        val operations: List<ApiSyncOperation>,
        val entityTypes: List<String>? = null
    )
    
    @Serializable
    data class ApiSyncOperation(
        val id: String,
        val entityType: String,
        val entityId: String,
        val operationType: String,
        val payload: String,
        val timestamp: Long
    )
    
    @Serializable
    data class ApiSyncResponse(
        val success: Boolean,
        val serverTimestamp: Long,
        val changes: List<ApiSyncChange>,
        val conflicts: List<ApiSyncConflict>,
        val errors: List<ApiSyncError>
    )
    
    @Serializable
    data class ApiSyncChange(
        val entityType: String,
        val entityId: String,
        val operationType: String,
        val data: String?,
        val version: Long,
        val lastModifiedAt: Long
    )
    
    @Serializable
    data class ApiSyncConflict(
        val entityType: String,
        val entityId: String,
        val localVersion: Long,
        val serverVersion: Long,
        val resolution: String,
        val resolvedData: String?
    )
    
    @Serializable
    data class ApiSyncError(
        val operationId: String,
        val errorCode: String,
        val errorMessage: String
    )
    
    // ========== API Methods ==========
    
    /**
     * Register device with the sync service
     */
    suspend fun registerDevice(request: DeviceRegistrationRequest): Result<DeviceRegistrationResponse> {
        return runCatching {
            val token = getAuthToken() ?: throw IllegalStateException("No auth token available")
            
            val apiRequest = ApiDeviceRegistrationRequest(
                deviceId = request.deviceId,
                deviceName = request.deviceName,
                deviceType = request.deviceType,
                deviceModel = request.deviceModel,
                osVersion = request.osVersion,
                appVersion = request.appVersion,
                pushToken = request.pushToken
            )
            
            val response: ApiDeviceRegistrationResponse = httpClient.post("${ApiConfig.syncApiUrl}/devices/register") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(apiRequest)
            }.body()
            
            DeviceRegistrationResponse(
                deviceId = response.deviceId,
                isNewDevice = response.isNewDevice,
                lastSyncTimestamp = response.lastSyncTimestamp
            )
        }
    }
    
    /**
     * Perform sync with server
     */
    suspend fun sync(request: SyncRequest): Result<SyncResponse> {
        return runCatching {
            val token = getAuthToken() ?: throw IllegalStateException("No auth token available")
            
            val apiRequest = ApiSyncRequest(
                deviceId = request.deviceId,
                lastSyncTimestamp = request.lastSyncTimestamp,
                operations = request.operations.map { op ->
                    ApiSyncOperation(
                        id = op.id,
                        entityType = op.entityType.name,
                        entityId = op.entityId,
                        operationType = op.operationType.name,
                        payload = op.payload,
                        timestamp = op.timestamp
                    )
                },
                entityTypes = request.entityTypes?.map { it.name }
            )
            
            val response: ApiSyncResponse = httpClient.post("${ApiConfig.syncApiUrl}/sync") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(apiRequest)
            }.body()
            
            response.toDomain()
        }
    }
    
    /**
     * Get changes since timestamp
     */
    suspend fun getChanges(deviceId: String, since: Long): Result<List<SyncChange>> {
        return runCatching {
            val token = getAuthToken() ?: throw IllegalStateException("No auth token available")
            
            val response: List<ApiSyncChange> = httpClient.get("${ApiConfig.syncApiUrl}/changes") {
                parameter("deviceId", deviceId)
                parameter("since", since)
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
            
            response.map { it.toDomain() }
        }
    }
    
    /**
     * Get sync status
     */
    suspend fun getSyncStatus(deviceId: String): Result<SyncState> {
        return runCatching {
            val token = getAuthToken() ?: throw IllegalStateException("No auth token available")
            
            val response: SyncStatusResponse = httpClient.get("${ApiConfig.syncApiUrl}/status") {
                parameter("deviceId", deviceId)
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
            
            SyncState(
                deviceId = response.deviceId,
                lastSyncTimestamp = response.lastSyncTimestamp,
                pendingOperationsCount = response.pendingChanges,
                isSyncing = false,
                lastSyncError = null
            )
        }
    }
    
    @Serializable
    private data class SyncStatusResponse(
        val deviceId: String,
        val lastSyncTimestamp: Long,
        val pendingChanges: Int
    )
    
    // ========== Conversion Functions ==========
    
    private fun ApiSyncResponse.toDomain(): SyncResponse {
        return SyncResponse(
            success = success,
            serverTimestamp = serverTimestamp,
            changes = changes.map { it.toDomain() },
            conflicts = conflicts.map { it.toDomain() },
            errors = errors.map { it.toDomain() }
        )
    }
    
    private fun ApiSyncChange.toDomain(): SyncChange {
        return SyncChange(
            entityType = SyncEntityType.valueOf(entityType),
            entityId = entityId,
            operationType = SyncOperationType.valueOf(operationType),
            data = data,
            metadata = SyncMetadata(
                entityType = SyncEntityType.valueOf(entityType),
                entityId = entityId,
                version = version,
                lastModifiedAt = lastModifiedAt,
                lastModifiedBy = null,
                isDeleted = operationType == "DELETE",
                checksum = null
            )
        )
    }
    
    private fun ApiSyncConflict.toDomain(): SyncConflict {
        return SyncConflict(
            entityType = SyncEntityType.valueOf(entityType),
            entityId = entityId,
            localVersion = localVersion,
            serverVersion = serverVersion,
            localData = "", // Backend doesn't return local data
            serverData = resolvedData ?: "",
            resolution = when (resolution) {
                "SERVER_WINS" -> ConflictResolution.SERVER_WINS
                "CLIENT_WINS" -> ConflictResolution.CLIENT_WINS
                "MERGED" -> ConflictResolution.MERGED
                else -> ConflictResolution.MANUAL_REQUIRED
            },
            resolvedData = resolvedData
        )
    }
    
    private fun ApiSyncError.toDomain(): SyncError {
        return SyncError(
            operationId = operationId,
            entityType = SyncEntityType.RESUME, // Default, backend doesn't return this
            entityId = "",
            errorCode = errorCode,
            errorMessage = errorMessage
        )
    }
}
