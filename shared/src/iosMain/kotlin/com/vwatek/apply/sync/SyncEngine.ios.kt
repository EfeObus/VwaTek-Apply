package com.vwatek.apply.sync

import com.vwatek.apply.data.api.SyncApiClient
import com.vwatek.apply.network.NetworkMonitor
import com.vwatek.apply.network.NetworkStatus
import io.ktor.client.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import platform.Foundation.*

/**
 * Platform-specific ID generation for iOS
 */
actual fun generateSyncOperationId(): String = NSUUID().UUIDString

/**
 * Platform-specific current time for iOS
 */
actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

/**
 * iOS factory for SyncEngine
 */
actual class SyncEngineFactory {
    private var httpClient: HttpClient? = null
    private var networkMonitor: NetworkMonitor? = null
    
    fun initialize(
        httpClient: HttpClient,
        networkMonitor: NetworkMonitor
    ) {
        this.httpClient = httpClient
        this.networkMonitor = networkMonitor
    }
    
    actual fun create(
        apiBaseUrl: String,
        authToken: () -> String?,
        onSyncComplete: ((SyncResponse) -> Unit)?,
        onSyncError: ((Throwable) -> Unit)?
    ): SyncEngine {
        val client = httpClient ?: throw IllegalStateException(
            "SyncEngineFactory must be initialized with httpClient first. " +
            "Call SyncEngineFactory().initialize(httpClient, networkMonitor) in your AppDelegate."
        )
        val monitor = networkMonitor ?: throw IllegalStateException("NetworkMonitor not initialized")
        
        return IosSyncEngine(
            httpClient = client,
            networkMonitor = monitor,
            getAuthToken = authToken,
            onSyncComplete = onSyncComplete,
            onSyncError = onSyncError
        )
    }
}

/**
 * iOS implementation of SyncEngine using NSUserDefaults for storage
 */
internal class IosSyncEngine(
    private val httpClient: HttpClient,
    private val networkMonitor: NetworkMonitor,
    private val getAuthToken: () -> String?,
    private val onSyncComplete: ((SyncResponse) -> Unit)?,
    private val onSyncError: ((Throwable) -> Unit)?
) : SyncEngine {
    
    private val defaults = NSUserDefaults.standardUserDefaults
    private val syncApiClient = SyncApiClient(httpClient, getAuthToken)
    
    private val _syncState = MutableStateFlow(
        SyncState(
            deviceId = getStoredDeviceId() ?: "",
            lastSyncTimestamp = getStoredLastSyncTimestamp(),
            pendingOperationsCount = 0,
            isSyncing = false,
            lastSyncError = null
        )
    )
    
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private val pendingOperations = mutableListOf<OfflineOperation>()
    private var autoSyncEnabled = true
    private var syncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    init {
        // Monitor network changes for auto-sync
        scope.launch {
            networkMonitor.networkState.collect { state ->
                if (autoSyncEnabled && state.status == NetworkStatus.AVAILABLE && pendingOperations.isNotEmpty()) {
                    syncNow()
                }
            }
        }
    }
    
    override suspend fun registerDevice(request: DeviceRegistrationRequest): Result<DeviceRegistrationResponse> {
        return syncApiClient.registerDevice(request).also { result ->
            result.onSuccess { response ->
                saveDeviceId(response.deviceId)
                response.lastSyncTimestamp?.let { saveLastSyncTimestamp(it) }
                updateState { it.copy(deviceId = response.deviceId) }
            }
        }
    }
    
    override suspend fun performFullSync(): Result<SyncResponse> {
        return performSync(lastSyncTimestamp = 0L)
    }
    
    override suspend fun performIncrementalSync(): Result<SyncResponse> {
        return performSync(lastSyncTimestamp = _syncState.value.lastSyncTimestamp)
    }
    
    private suspend fun performSync(lastSyncTimestamp: Long): Result<SyncResponse> {
        if (_syncState.value.isSyncing) {
            return Result.failure(IllegalStateException("Sync already in progress"))
        }
        
        updateState { it.copy(isSyncing = true, lastSyncError = null) }
        
        val request = SyncRequest(
            deviceId = _syncState.value.deviceId,
            lastSyncTimestamp = lastSyncTimestamp,
            operations = pendingOperations.toList(),
            entityTypes = null
        )
        
        return syncApiClient.sync(request).also { result ->
            result.onSuccess { response ->
                if (response.success) {
                    saveLastSyncTimestamp(response.serverTimestamp)
                    val errorIds = response.errors.map { it.operationId }.toSet()
                    pendingOperations.removeAll { it.id !in errorIds }
                    
                    updateState {
                        it.copy(
                            isSyncing = false,
                            lastSyncTimestamp = response.serverTimestamp,
                            pendingOperationsCount = pendingOperations.size,
                            lastSyncError = null
                        )
                    }
                    onSyncComplete?.invoke(response)
                } else {
                    val errorMsg = response.errors.firstOrNull()?.errorMessage ?: "Sync failed"
                    updateState { it.copy(isSyncing = false, lastSyncError = errorMsg) }
                    onSyncError?.invoke(Exception(errorMsg))
                }
            }.onFailure { error ->
                updateState { it.copy(isSyncing = false, lastSyncError = error.message) }
                onSyncError?.invoke(error)
            }
        }
    }
    
    override suspend fun queueOperation(operation: OfflineOperation) {
        pendingOperations.add(operation)
        updateState { it.copy(pendingOperationsCount = pendingOperations.size) }
        
        if (autoSyncEnabled && networkMonitor.networkState.value.status == NetworkStatus.AVAILABLE) {
            syncNow()
        }
    }
    
    override suspend fun getPendingOperations(): List<OfflineOperation> = pendingOperations.toList()
    
    override suspend fun clearPendingOperations(operationIds: List<String>) {
        val idsSet = operationIds.toSet()
        pendingOperations.removeAll { it.id in idsSet }
        updateState { it.copy(pendingOperationsCount = pendingOperations.size) }
    }
    
    override suspend fun hasPendingOperations(): Boolean = pendingOperations.isNotEmpty()
    
    override fun setAutoSyncEnabled(enabled: Boolean) {
        autoSyncEnabled = enabled
    }
    
    override suspend fun syncNow(): Result<SyncResponse> {
        return performIncrementalSync()
    }
    
    override fun cancelSync() {
        syncJob?.cancel()
        updateState { it.copy(isSyncing = false) }
    }
    
    private fun updateState(transform: (SyncState) -> SyncState) {
        _syncState.value = transform(_syncState.value)
    }
    
    private fun getStoredDeviceId(): String? = defaults.stringForKey("vwatek_device_id")
    
    private fun saveDeviceId(deviceId: String) {
        defaults.setObject(deviceId, "vwatek_device_id")
    }
    
    private fun getStoredLastSyncTimestamp(): Long = 
        defaults.doubleForKey("vwatek_last_sync_timestamp").toLong()
    
    private fun saveLastSyncTimestamp(timestamp: Long) {
        defaults.setDouble(timestamp.toDouble(), "vwatek_last_sync_timestamp")
    }
}
