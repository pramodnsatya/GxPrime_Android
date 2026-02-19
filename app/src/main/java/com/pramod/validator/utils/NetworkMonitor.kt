package com.pramod.validator.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Monitors network connectivity and provides a Flow of connectivity status
 */
class NetworkMonitor(private val context: Context) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    /**
     * Flow that emits true when online, false when offline
     */
    val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }
            
            override fun onLost(network: Network) {
                trySend(false)
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                // Only check for INTERNET capability, not VALIDATED
                // VALIDATED can be slow or fail even when connected
                val hasInternet = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
                trySend(hasInternet)
            }
        }
        
        // Register network callback
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, callback)
        
        // Emit initial state
        trySend(isCurrentlyOnline())
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
    
    /**
     * Check current connectivity status synchronously
     * Uses a less strict check - only requires INTERNET capability, not VALIDATED
     * This is because VALIDATED can be slow or fail even when connected
     */
    fun isCurrentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        // Check if network has internet capability
        // We check for VALIDATED if available, but don't require it
        // This allows the check to work even when validation is slow
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        
        // Return true if we have internet capability, even if not yet validated
        // Validation will happen when we actually try to use the network
        return hasInternet || isValidated
    }
}

