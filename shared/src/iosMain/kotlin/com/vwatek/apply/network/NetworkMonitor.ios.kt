package com.vwatek.apply.network

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.*
import platform.Network.*
import platform.darwin.dispatch_get_main_queue

/**
 * iOS implementation of NetworkMonitor using NWPathMonitor
 */
class IOSNetworkMonitor : NetworkMonitor {
    
    private val pathMonitor = nw_path_monitor_create()
    private val monitorQueue = dispatch_get_main_queue()
    
    private val _networkState = MutableStateFlow(NetworkState.UNKNOWN)
    override val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    override val isConnected: Boolean
        get() = _networkState.value.isConnected
    
    private val onConnectedListeners = mutableListOf<() -> Unit>()
    private val onDisconnectedListeners = mutableListOf<() -> Unit>()
    
    override val connectivityChanges: Flow<NetworkState> = callbackFlow {
        nw_path_monitor_set_update_handler(pathMonitor) { path ->
            val state = pathToNetworkState(path)
            trySend(state)
        }
        
        nw_path_monitor_set_queue(pathMonitor, monitorQueue)
        nw_path_monitor_start(pathMonitor)
        
        awaitClose {
            nw_path_monitor_cancel(pathMonitor)
        }
    }
    
    override fun startMonitoring() {
        nw_path_monitor_set_update_handler(pathMonitor) { path ->
            val wasDisconnected = !_networkState.value.isConnected
            val newState = pathToNetworkState(path)
            _networkState.value = newState
            
            if (newState.isConnected && wasDisconnected) {
                onConnectedListeners.forEach { it() }
            } else if (!newState.isConnected && !wasDisconnected) {
                onDisconnectedListeners.forEach { it() }
            }
        }
        
        nw_path_monitor_set_queue(pathMonitor, monitorQueue)
        nw_path_monitor_start(pathMonitor)
    }
    
    override fun stopMonitoring() {
        nw_path_monitor_cancel(pathMonitor)
    }
    
    override suspend fun checkConnectivity(): Boolean {
        // Use current state from path monitor
        return _networkState.value.isConnected
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
    
    private fun pathToNetworkState(path: nw_path_t?): NetworkState {
        if (path == null) return NetworkState.UNKNOWN
        
        val status = when (nw_path_get_status(path)) {
            nw_path_status_satisfied -> NetworkStatus.AVAILABLE
            nw_path_status_unsatisfied -> NetworkStatus.UNAVAILABLE
            nw_path_status_satisfiable -> NetworkStatus.LOSING
            else -> NetworkStatus.UNAVAILABLE
        }
        
        val type = when {
            nw_path_uses_interface_type(path, nw_interface_type_wifi) -> NetworkType.WIFI
            nw_path_uses_interface_type(path, nw_interface_type_cellular) -> NetworkType.CELLULAR
            nw_path_uses_interface_type(path, nw_interface_type_wired) -> NetworkType.ETHERNET
            else -> NetworkType.UNKNOWN
        }
        
        val isExpensive = nw_path_is_expensive(path)
        val isConstrained = nw_path_is_constrained(path)
        
        return NetworkState(
            status = status,
            type = type,
            isMetered = isExpensive || isConstrained,
            isRoaming = false // iOS doesn't expose roaming status directly
        )
    }
}

/**
 * iOS factory for NetworkMonitor
 */
actual class NetworkMonitorFactory {
    actual fun create(): NetworkMonitor {
        return IOSNetworkMonitor()
    }
}
