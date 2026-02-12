package com.vwatek.apply.network

import kotlinx.browser.window
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import org.w3c.dom.events.Event

/**
 * JavaScript/Web implementation of NetworkMonitor using Navigator.onLine and Network Information API
 */
class WebNetworkMonitor : NetworkMonitor {
    
    private val _networkState = MutableStateFlow(getCurrentNetworkState())
    override val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    override val isConnected: Boolean
        get() = _networkState.value.isConnected
    
    private val onConnectedListeners = mutableListOf<() -> Unit>()
    private val onDisconnectedListeners = mutableListOf<() -> Unit>()
    
    private var onlineHandler: ((Event) -> Unit)? = null
    private var offlineHandler: ((Event) -> Unit)? = null
    
    override val connectivityChanges: Flow<NetworkState> = callbackFlow {
        val onOnline: (Event) -> Unit = {
            trySend(getCurrentNetworkState())
        }
        
        val onOffline: (Event) -> Unit = {
            trySend(NetworkState.DISCONNECTED)
        }
        
        window.addEventListener("online", onOnline)
        window.addEventListener("offline", onOffline)
        
        // Also listen for connection type changes if Network Information API is available
        val connection = getNetworkConnection()
        val connectionChangeHandler: ((Event) -> Unit)? = if (connection != null) {
            val handler: (Event) -> Unit = {
                trySend(getCurrentNetworkState())
            }
            connection.addEventListener("change", handler)
            handler
        } else null
        
        awaitClose {
            window.removeEventListener("online", onOnline)
            window.removeEventListener("offline", onOffline)
            if (connection != null && connectionChangeHandler != null) {
                connection.removeEventListener("change", connectionChangeHandler)
            }
        }
    }
    
    override fun startMonitoring() {
        onlineHandler = { _: Event ->
            val wasDisconnected = !_networkState.value.isConnected
            _networkState.value = getCurrentNetworkState()
            
            if (wasDisconnected) {
                onConnectedListeners.forEach { it() }
            }
        }
        
        offlineHandler = { _: Event ->
            _networkState.value = NetworkState.DISCONNECTED
            onDisconnectedListeners.forEach { it() }
        }
        
        window.addEventListener("online", onlineHandler!!)
        window.addEventListener("offline", offlineHandler!!)
        
        // Initial state check
        _networkState.value = getCurrentNetworkState()
    }
    
    override fun stopMonitoring() {
        onlineHandler?.let { window.removeEventListener("online", it) }
        offlineHandler?.let { window.removeEventListener("offline", it) }
        onlineHandler = null
        offlineHandler = null
    }
    
    override suspend fun checkConnectivity(): Boolean {
        return try {
            // Try to fetch a small resource to verify actual connectivity
            val navigator = window.navigator
            if (!navigator.onLine) return false
            
            // Additional connectivity check could be implemented with fetch()
            // For now, trust navigator.onLine
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun addOnConnectedListener(listener: () -> Unit) {
        onConnectedListeners.add(listener)
    }
    
    override fun addOnDisconnectedListener(listener: () -> Unit) {
        onDisconnectedListeners.add(listener)
    }
    
    override fun removeAllListeners() {
        onConnectedListeners.clear()
        onDisconnectedListeners.clear()
    }
    
    private fun getCurrentNetworkState(): NetworkState {
        val isOnline = window.navigator.onLine
        
        if (!isOnline) {
            return NetworkState.DISCONNECTED
        }
        
        // Try to get more detailed info from Network Information API
        val connection = getNetworkConnection()
        
        return if (connection != null) {
            val type = getNetworkType(connection)
            val downlink = getDownlink(connection)
            
            NetworkState(
                status = NetworkStatus.AVAILABLE,
                type = type,
                isMetered = isMeteredConnection(connection),
                downloadSpeedMbps = downlink,
                uploadSpeedMbps = -1f // Not available in Network Information API
            )
        } else {
            // Network Information API not available
            NetworkState(
                status = NetworkStatus.AVAILABLE,
                type = NetworkType.UNKNOWN
            )
        }
    }
    
    private fun getNetworkConnection(): dynamic {
        return try {
            val navigator = window.navigator.asDynamic()
            navigator.connection ?: navigator.mozConnection ?: navigator.webkitConnection
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getNetworkType(connection: dynamic): NetworkType {
        return try {
            when (connection.type as? String) {
                "wifi" -> NetworkType.WIFI
                "cellular" -> NetworkType.CELLULAR
                "ethernet" -> NetworkType.ETHERNET
                "none" -> NetworkType.NONE
                else -> {
                    // Fallback to effectiveType
                    when (connection.effectiveType as? String) {
                        "slow-2g", "2g", "3g" -> NetworkType.CELLULAR
                        "4g" -> NetworkType.CELLULAR
                        else -> NetworkType.UNKNOWN
                    }
                }
            }
        } catch (e: Exception) {
            NetworkType.UNKNOWN
        }
    }
    
    private fun getDownlink(connection: dynamic): Float {
        return try {
            (connection.downlink as? Number)?.toFloat() ?: -1f
        } catch (e: Exception) {
            -1f
        }
    }
    
    private fun isMeteredConnection(connection: dynamic): Boolean {
        return try {
            connection.saveData == true || 
            (connection.effectiveType as? String) in listOf("slow-2g", "2g", "3g")
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Web factory for NetworkMonitor
 */
actual class NetworkMonitorFactory {
    actual fun create(): NetworkMonitor {
        return WebNetworkMonitor()
    }
}
