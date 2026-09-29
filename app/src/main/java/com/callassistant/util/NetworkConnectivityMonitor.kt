package com.callassistant.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

const val CLOUD_AI_INTERNET_REQUIRED_MESSAGE =
    "No internet connection. Connect to Wi‑Fi or mobile data to use cloud AI suggestions."

@Singleton
class NetworkConnectivityMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isOnline(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            (
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
    }
}

fun isNetworkRelatedError(error: Throwable): Boolean {
    var current: Throwable? = error
    while (current != null) {
        when (current) {
            is UnknownHostException,
            is ConnectException,
            is SocketTimeoutException -> return true
            is IOException -> {
                val message = current.message.orEmpty().lowercase()
                if (
                    message.contains("unable to resolve host") ||
                    message.contains("failed to connect") ||
                    message.contains("network is unreachable") ||
                    message.contains("connection refused") ||
                    message.contains("timeout") ||
                    message.contains("no address associated with hostname")
                ) {
                    return true
                }
            }
        }
        current = current.cause
    }
    return false
}

fun cloudAiNetworkErrorMessage(error: Throwable): String =
    if (isNetworkRelatedError(error)) {
        CLOUD_AI_INTERNET_REQUIRED_MESSAGE
    } else {
        error.message ?: "Cloud AI request failed"
    }
