package com.vwatek.apply.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Network Monitor for VwaTek Apply
 * 
 * Cross-platform network connectivity monitoring with:
 * - Real-time connection status updates
 * - Network type detection (WiFi, Cellular, etc.)
 * - Sync-aware connectivity (triggers sync when coming online)
 */

/**
 * Represents the current network status
 */
enum class NetworkStatus {
    AVAILABLE,      // Network is available and connected
    UNAVAILABLE,    // No network connection
    LOSING,         // Network is about to be lost
    LOST            // Network connection was lost
}

/**
 * Represents the type of network connection
 */
enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    VPN,
    UNKNOWN,
    NONE
}

/**
 * Detailed network state including quality information
 */
data class NetworkState(
    val status: NetworkStatus,
    val type: NetworkType,
    val isMetered: Boolean = false,       // True if connection has data limits
    val isRoaming: Boolean = false,       // True if on roaming network
    val signalStrength: Int = -1,         // Signal strength (0-100, -1 if unknown)
    val downloadSpeedMbps: Float = -1f,   // Estimated download speed
    val uploadSpeedMbps: Float = -1f      // Estimated upload speed
) {
    val isConnected: Boolean
        get() = status == NetworkStatus.AVAILABLE
    
    val isFastConnection: Boolean
        get() = type == NetworkType.WIFI || type == NetworkType.ETHERNET ||
                (type == NetworkType.CELLULAR && downloadSpeedMbps > 5f)
    
    val shouldSync: Boolean
        get() = isConnected && !isMetered && !isRoaming
    
    companion object {
        val DISCONNECTED = NetworkState(
            status = NetworkStatus.UNAVAILABLE,
            type = NetworkType.NONE
        )
        
        val UNKNOWN = NetworkState(
            status = NetworkStatus.UNAVAILABLE,
            type = NetworkType.UNKNOWN
        )
    }
}

/**
 * Platform-independent Network Monitor interface
 */
interface NetworkMonitor {
    /**
     * Current network state as a reactive flow
     */
    val networkState: StateFlow<NetworkState>
    
    /**
     * Quick check if network is currently available
     */
    val isConnected: Boolean
    
    /**
     * Flow of connectivity changes
     */
    val connectivityChanges: Flow<NetworkState>
    
    /**
     * Start monitoring network changes
     */
    fun startMonitoring()
    
    /**
     * Stop monitoring network changes
     */
    fun stopMonitoring()
    
    /**
     * Perform a connectivity check by pinging a server
     */
    suspend fun checkConnectivity(): Boolean
    
    /**
     * Add a listener for when network becomes available
     */
    fun addOnConnectedListener(listener: () -> Unit)
    
    /**
     * Add a listener for when network becomes unavailable
     */
    fun addOnDisconnectedListener(listener: () -> Unit)
    
    /**
     * Remove all listeners
     */
    fun removeAllListeners()
}

/**
 * Factory for creating platform-specific NetworkMonitor instances
 */
expect class NetworkMonitorFactory {
    fun create(): NetworkMonitor
}

/**
 * Sync-aware network observer that triggers actions based on connectivity
 */
class SyncNetworkObserver(
    private val networkMonitor: NetworkMonitor,
    private val onNetworkAvailable: suspend () -> Unit,
    private val onNetworkLost: () -> Unit
) {
    private var wasOffline = false
    
    fun observe() {
        networkMonitor.addOnConnectedListener {
            if (wasOffline) {
                // We just came back online - trigger sync
                kotlinx.coroutines.GlobalScope.launch {
                    onNetworkAvailable()
                }
            }
            wasOffline = false
        }
        
        networkMonitor.addOnDisconnectedListener {
            wasOffline = true
            onNetworkLost()
        }
        
        networkMonitor.startMonitoring()
    }
    
    fun stop() {
        networkMonitor.removeAllListeners()
        networkMonitor.stopMonitoring()
    }
}

// Import for GlobalScope
private object kotlinx {
    object coroutines {
        object GlobalScope {
            fun launch(block: suspend () -> Unit) {
                // Platform-specific implementation will override this
            }
        }
    }
}
