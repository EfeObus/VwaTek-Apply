package com.vwatek.apply.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Android implementation of NetworkMonitor using ConnectivityManager
 */
class AndroidNetworkMonitor(private val context: Context) : NetworkMonitor {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _networkState = MutableStateFlow(getCurrentNetworkState())
    override val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    override val isConnected: Boolean
        get() = _networkState.value.isConnected
    
    private val onConnectedListeners = mutableListOf<() -> Unit>()
    private val onDisconnectedListeners = mutableListOf<() -> Unit>()
    
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    override val connectivityChanges: Flow<NetworkState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val state = getNetworkState(network)
                trySend(state)
            }
            
            override fun onLost(network: Network) {
                trySend(NetworkState.DISCONNECTED)
            }
            
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val state = getNetworkStateFromCapabilities(capabilities)
                trySend(state)
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
    
    override fun startMonitoring() {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val state = getNetworkState(network)
                val wasDisconnected = !_networkState.value.isConnected
                _networkState.value = state
                
                if (wasDisconnected) {
                    onConnectedListeners.forEach { it() }
                }
            }
            
            override fun onLost(network: Network) {
                val newState = NetworkState(
                    status = NetworkStatus.LOST,
                    type = NetworkType.NONE
                )
                _networkState.value = newState
                onDisconnectedListeners.forEach { it() }
            }
            
            override fun onLosing(network: Network, maxMsToLive: Int) {
                _networkState.value = _networkState.value.copy(status = NetworkStatus.LOSING)
            }
            
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val state = getNetworkStateFromCapabilities(capabilities)
                _networkState.value = state
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }
    
    override fun stopMonitoring() {
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
            networkCallback = null
        }
    }
    
    override suspend fun checkConnectivity(): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("8.8.8.8", 53), 1500)
                true
            }
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
        val activeNetwork = connectivityManager.activeNetwork ?: return NetworkState.DISCONNECTED
        return getNetworkState(activeNetwork)
    }
    
    private fun getNetworkState(network: Network): NetworkState {
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return NetworkState.DISCONNECTED
        return getNetworkStateFromCapabilities(capabilities)
    }
    
    private fun getNetworkStateFromCapabilities(capabilities: NetworkCapabilities): NetworkState {
        val type = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
            else -> NetworkType.UNKNOWN
        }
        
        val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val isRoaming = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
        
        // Get bandwidth estimates if available (API 21+)
        val downloadMbps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            capabilities.linkDownstreamBandwidthKbps / 1000f
        } else -1f
        
        val uploadMbps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            capabilities.linkUpstreamBandwidthKbps / 1000f
        } else -1f
        
        // Get signal strength if available (API 29+)
        val signalStrength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            capabilities.signalStrength.coerceIn(0, 100)
        } else -1
        
        return NetworkState(
            status = NetworkStatus.AVAILABLE,
            type = type,
            isMetered = isMetered,
            isRoaming = isRoaming,
            signalStrength = signalStrength,
            downloadSpeedMbps = downloadMbps,
            uploadSpeedMbps = uploadMbps
        )
    }
}

/**
 * Android factory for NetworkMonitor
 */
actual class NetworkMonitorFactory {
    private var context: Context? = null
    
    fun initialize(context: Context) {
        this.context = context.applicationContext
    }
    
    actual fun create(): NetworkMonitor {
        val ctx = context ?: throw IllegalStateException(
            "NetworkMonitorFactory must be initialized with context first. " +
            "Call NetworkMonitorFactory().initialize(context) in your Application class."
        )
        return AndroidNetworkMonitor(ctx)
    }
}
