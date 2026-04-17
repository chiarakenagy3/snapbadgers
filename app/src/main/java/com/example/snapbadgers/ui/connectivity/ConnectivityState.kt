package com.example.snapbadgers.ui.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.awaitDispose
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberIsOnline(): State<Boolean> {
    val appContext = LocalContext.current.applicationContext
    val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    return produceState(initialValue = connectivityManager.isCurrentlyOnline(), connectivityManager) {
        value = connectivityManager.isCurrentlyOnline()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                value = connectivityManager.isCurrentlyOnline()
            }

            override fun onLost(network: Network) {
                value = connectivityManager.isCurrentlyOnline()
            }

            override fun onUnavailable() {
                value = false
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        awaitDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}

private fun ConnectivityManager.isCurrentlyOnline(): Boolean {
    val activeNetwork = activeNetwork ?: return false
    val capabilities = getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
