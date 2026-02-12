package com.vwatek.apply.routes

import com.vwatek.apply.db.tables.*
import com.vwatek.apply.plugins.MetricsHelper
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/**
 * Sync API Routes for VwaTek Apply
 * 
 * Provides endpoints for:
 * - Device registration
 * - Data synchronization (push/pull)
 * - Conflict resolution
 */

// Request/Response models
@Serializable
data class DeviceRegistrationRequest(
    val deviceId: String? = null,
    val deviceName: String,
    val deviceType: String,
    val deviceModel: String? = null,
    val osVersion: String? = null,
    val appVersion: String,
    val pushToken: String? = null
)

@Serializable
data class DeviceRegistrationResponse(
    val deviceId: String,
    val isNewDevice: Boolean,
    val lastSyncTimestamp: Long?
)

@Serializable
data class SyncRequest(
    val deviceId: String,
    val lastSyncTimestamp: Long,
    val operations: List<SyncOperation>,
    val entityTypes: List<String>? = null
)

@Serializable
data class SyncOperation(
    val id: String,
    val entityType: String,
    val entityId: String,
    val operationType: String, // CREATE, UPDATE, DELETE
    val payload: String,
    val timestamp: Long
)

@Serializable
data class SyncResponse(
    val success: Boolean,
    val serverTimestamp: Long,
    val changes: List<SyncChange>,
    val conflicts: List<SyncConflict>,
    val errors: List<SyncError>
)

@Serializable
data class SyncChange(
    val entityType: String,
    val entityId: String,
    val operationType: String,
    val data: String?,
    val version: Long,
    val lastModifiedAt: Long
)

@Serializable
data class SyncConflict(
    val entityType: String,
    val entityId: String,
    val localVersion: Long,
    val serverVersion: Long,
    val resolution: String,
    val resolvedData: String?
)

@Serializable
data class SyncError(
    val operationId: String,
    val errorCode: String,
    val errorMessage: String
)

fun Route.syncRoutes() {
    route("/api/sync") {
        
        // Device registration
        post("/devices/register") {
            val request = call.receive<DeviceRegistrationRequest>()
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
            
            val result = MetricsHelper.recordDbQuery {
                transaction {
                    val now = Clock.System.now().toJavaInstant()
                    val nowKotlin = kotlinx.datetime.Instant.fromEpochMilliseconds(now.toEpochMilli())
                    
                    // Check if device already exists
                    val existingDeviceId = request.deviceId
                    val existingDevice = if (existingDeviceId != null) {
                        DevicesTable.select { 
                            (DevicesTable.id eq existingDeviceId) and (DevicesTable.userId eq userId) 
                        }.singleOrNull()
                    } else null
                    
                    if (existingDevice != null) {
                        // Update existing device
                        DevicesTable.update({ DevicesTable.id eq existingDeviceId!! }) {
                            it[deviceName] = request.deviceName
                            it[appVersion] = request.appVersion
                            it[pushToken] = request.pushToken
                            it[lastActiveAt] = nowKotlin
                            it[updatedAt] = nowKotlin
                            it[isActive] = true
                        }
                        
                        val lastSync = existingDevice[DevicesTable.lastSyncAt]?.toEpochMilliseconds()
                        DeviceRegistrationResponse(
                            deviceId = existingDeviceId!!,
                            isNewDevice = false,
                            lastSyncTimestamp = lastSync
                        )
                    } else {
                        // Create new device
                        val newDeviceId = request.deviceId ?: UUID.randomUUID().toString()
                        
                        DevicesTable.insert {
                            it[id] = newDeviceId
                            it[DevicesTable.userId] = userId
                            it[deviceName] = request.deviceName
                            it[deviceType] = request.deviceType
                            it[deviceModel] = request.deviceModel
                            it[osVersion] = request.osVersion
                            it[appVersion] = request.appVersion
                            it[pushToken] = request.pushToken
                            it[lastActiveAt] = nowKotlin
                            it[isActive] = true
                            it[createdAt] = nowKotlin
                            it[updatedAt] = nowKotlin
                        }
                        
                        DeviceRegistrationResponse(
                            deviceId = newDeviceId,
                            isNewDevice = true,
                            lastSyncTimestamp = null
                        )
                    }
                }
            }
            
            call.respond(HttpStatusCode.OK, result)
        }
        
        // Main sync endpoint
        post("/sync") {
            val request = call.receive<SyncRequest>()
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
            
            val response = MetricsHelper.recordSyncOperation {
                performSync(userId, request)
            }
            
            call.respond(HttpStatusCode.OK, response)
        }
        
        // Get changes since timestamp (pull only)
        get("/changes") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
            
            val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
            val entityTypes = call.request.queryParameters["types"]?.split(",")
            
            val changes = getChangesSince(userId, since, entityTypes)
            
            call.respond(HttpStatusCode.OK, mapOf(
                "serverTimestamp" to Clock.System.now().toEpochMilliseconds(),
                "changes" to changes
            ))
        }
        
        // Get sync status
        get("/status") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
            
            val deviceId = call.request.queryParameters["deviceId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "deviceId required"))
            
            val status = transaction {
                DevicesTable.select { 
                    (DevicesTable.id eq deviceId) and (DevicesTable.userId eq userId) 
                }.singleOrNull()?.let { device ->
                    mapOf(
                        "deviceId" to deviceId,
                        "lastSyncAt" to device[DevicesTable.lastSyncAt]?.toEpochMilliseconds(),
                        "isActive" to device[DevicesTable.isActive]
                    )
                }
            }
            
            if (status != null) {
                call.respond(HttpStatusCode.OK, status)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Device not found"))
            }
        }
    }
}

private fun performSync(userId: String, request: SyncRequest): SyncResponse {
    val changes = mutableListOf<SyncChange>()
    val conflicts = mutableListOf<SyncConflict>()
    val errors = mutableListOf<SyncError>()
    val serverTimestamp = Clock.System.now().toEpochMilliseconds()
    
    // Create sync log entry
    val syncLogId = UUID.randomUUID().toString()
    val startTime = System.currentTimeMillis()
    
    transaction {
        val now = kotlinx.datetime.Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
        
        SyncLogsTable.insert {
            it[id] = syncLogId
            it[SyncLogsTable.userId] = userId
            it[deviceId] = request.deviceId
            it[syncType] = if (request.lastSyncTimestamp == 0L) "FULL" else "INCREMENTAL"
            it[status] = "STARTED"
            it[startedAt] = now
        }
    }
    
    try {
        // Process incoming operations (push)
        var itemsPushed = 0
        var conflictsResolved = 0
        
        for (operation in request.operations) {
            try {
                val result = processOperation(userId, request.deviceId, operation)
                when (result) {
                    is OperationResult.Success -> itemsPushed++
                    is OperationResult.Conflict -> {
                        conflicts.add(result.conflict)
                        conflictsResolved++
                    }
                    is OperationResult.Error -> {
                        errors.add(result.error)
                    }
                }
            } catch (e: Exception) {
                errors.add(SyncError(
                    operationId = operation.id,
                    errorCode = "PROCESSING_ERROR",
                    errorMessage = e.message ?: "Unknown error"
                ))
            }
        }
        
        // Get changes for client (pull)
        val entityTypes = request.entityTypes
        val serverChanges = getChangesSince(userId, request.lastSyncTimestamp, entityTypes)
        changes.addAll(serverChanges)
        
        // Update device last sync time
        transaction {
            val now = kotlinx.datetime.Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
            DevicesTable.update({ DevicesTable.id eq request.deviceId }) {
                it[lastSyncAt] = now
                it[lastActiveAt] = now
            }
            
            // Complete sync log
            SyncLogsTable.update({ SyncLogsTable.id eq syncLogId }) {
                it[status] = "COMPLETED"
                it[SyncLogsTable.itemsPushed] = itemsPushed
                it[itemsPulled] = changes.size
                it[SyncLogsTable.conflictsResolved] = conflictsResolved
                it[durationMs] = System.currentTimeMillis() - startTime
                it[completedAt] = now
            }
        }
        
    } catch (e: Exception) {
        // Log sync failure
        transaction {
            val now = kotlinx.datetime.Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
            SyncLogsTable.update({ SyncLogsTable.id eq syncLogId }) {
                it[status] = "FAILED"
                it[errorMessage] = e.message
                it[durationMs] = System.currentTimeMillis() - startTime
                it[completedAt] = now
            }
        }
        throw e
    }
    
    return SyncResponse(
        success = errors.isEmpty(),
        serverTimestamp = serverTimestamp,
        changes = changes,
        conflicts = conflicts,
        errors = errors
    )
}

private sealed class OperationResult {
    data class Success(val metadata: SyncChange) : OperationResult()
    data class Conflict(val conflict: SyncConflict) : OperationResult()
    data class Error(val error: SyncError) : OperationResult()
}

private fun processOperation(userId: String, deviceId: String, operation: SyncOperation): OperationResult {
    return transaction {
        val now = kotlinx.datetime.Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
        
        // Check for existing sync metadata
        val existingMetadata = SyncMetadataTable.select {
            (SyncMetadataTable.userId eq userId) and
            (SyncMetadataTable.entityType eq operation.entityType) and
            (SyncMetadataTable.entityId eq operation.entityId)
        }.singleOrNull()
        
        if (existingMetadata != null) {
            val serverVersion = existingMetadata[SyncMetadataTable.version]
            val serverModifiedAt = existingMetadata[SyncMetadataTable.lastModifiedAt].toEpochMilliseconds()
            
            // Conflict detection: if server version is newer, it's a conflict
            if (serverModifiedAt > operation.timestamp) {
                // Last-write-wins: server version wins
                return@transaction OperationResult.Conflict(
                    SyncConflict(
                        entityType = operation.entityType,
                        entityId = operation.entityId,
                        localVersion = 0, // Client doesn't track versions
                        serverVersion = serverVersion,
                        resolution = "SERVER_WINS",
                        resolvedData = null // Client should fetch current server version
                    )
                )
            }
        }
        
        // Apply the operation
        when (operation.operationType) {
            "CREATE", "UPDATE" -> {
                // Apply to actual entity table based on entityType
                applyEntityChange(userId, operation)
                
                // Update sync metadata
                if (existingMetadata != null) {
                    SyncMetadataTable.update({
                        (SyncMetadataTable.userId eq userId) and
                        (SyncMetadataTable.entityType eq operation.entityType) and
                        (SyncMetadataTable.entityId eq operation.entityId)
                    }) {
                        it[version] = existingMetadata[SyncMetadataTable.version] + 1
                        it[lastModifiedAt] = now
                        it[lastModifiedBy] = deviceId
                        it[isDeleted] = false
                    }
                } else {
                    SyncMetadataTable.insert {
                        it[id] = UUID.randomUUID().toString()
                        it[SyncMetadataTable.userId] = userId
                        it[entityType] = operation.entityType
                        it[entityId] = operation.entityId
                        it[version] = 1
                        it[lastModifiedAt] = now
                        it[lastModifiedBy] = deviceId
                        it[createdAt] = now
                    }
                }
            }
            "DELETE" -> {
                // Soft delete via sync metadata
                if (existingMetadata != null) {
                    SyncMetadataTable.update({
                        (SyncMetadataTable.userId eq userId) and
                        (SyncMetadataTable.entityType eq operation.entityType) and
                        (SyncMetadataTable.entityId eq operation.entityId)
                    }) {
                        it[version] = existingMetadata[SyncMetadataTable.version] + 1
                        it[lastModifiedAt] = now
                        it[lastModifiedBy] = deviceId
                        it[isDeleted] = true
                        it[deletedAt] = now
                    }
                }
            }
        }
        
        // Add to change feed for other devices
        ChangeFeedTable.insert {
            it[ChangeFeedTable.userId] = userId
            it[entityType] = operation.entityType
            it[entityId] = operation.entityId
            it[changeType] = operation.operationType
            it[changedAt] = now
        }
        
        OperationResult.Success(
            SyncChange(
                entityType = operation.entityType,
                entityId = operation.entityId,
                operationType = operation.operationType,
                data = operation.payload,
                version = 1,
                lastModifiedAt = now.toEpochMilliseconds()
            )
        )
    }
}

private fun applyEntityChange(userId: String, operation: SyncOperation) {
    // This would be expanded to handle each entity type
    // For now, entities are synced through their respective endpoints
    // This function updates the sync metadata only
}

private fun getChangesSince(userId: String, since: Long, entityTypes: List<String>?): List<SyncChange> {
    return transaction {
        val sinceInstant = kotlinx.datetime.Instant.fromEpochMilliseconds(since)
        
        var query = SyncMetadataTable.select {
            (SyncMetadataTable.userId eq userId) and
            (SyncMetadataTable.lastModifiedAt greater sinceInstant)
        }
        
        if (!entityTypes.isNullOrEmpty()) {
            query = query.andWhere { SyncMetadataTable.entityType inList entityTypes }
        }
        
        query.map { row ->
            SyncChange(
                entityType = row[SyncMetadataTable.entityType],
                entityId = row[SyncMetadataTable.entityId],
                operationType = if (row[SyncMetadataTable.isDeleted]) "DELETE" else "UPDATE",
                data = null, // Client should fetch full entity data
                version = row[SyncMetadataTable.version],
                lastModifiedAt = row[SyncMetadataTable.lastModifiedAt].toEpochMilliseconds()
            )
        }
    }
}
