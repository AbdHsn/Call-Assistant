package com.callassistant.incall

import android.telecom.CallAudioState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.graphics.vector.ImageVector

data class CallAudioRouteOption(
    val route: Int,
    val label: String,
    val icon: ImageVector
)

fun supportedCallAudioRoutes(mask: Int, bluetoothName: String? = null): List<CallAudioRouteOption> {
    val routes = mutableListOf<CallAudioRouteOption>()
    if (mask and CallAudioState.ROUTE_EARPIECE != 0) {
        routes += CallAudioRouteOption(CallAudioState.ROUTE_EARPIECE, "Phone", Icons.Filled.Phone)
    }
    if (mask and CallAudioState.ROUTE_SPEAKER != 0) {
        routes += CallAudioRouteOption(CallAudioState.ROUTE_SPEAKER, "Speaker", Icons.AutoMirrored.Filled.VolumeUp)
    }
    if (mask and CallAudioState.ROUTE_BLUETOOTH != 0) {
        val label = bluetoothName?.takeIf { it.isNotBlank() } ?: "Bluetooth"
        routes += CallAudioRouteOption(CallAudioState.ROUTE_BLUETOOTH, label, Icons.Filled.Bluetooth)
    }
    if (mask and CallAudioState.ROUTE_WIRED_HEADSET != 0) {
        routes += CallAudioRouteOption(CallAudioState.ROUTE_WIRED_HEADSET, "Headphones", Icons.Filled.Headphones)
    }
    return routes
}

fun callAudioRouteLabel(route: Int, bluetoothName: String? = null): String = when (route) {
    CallAudioState.ROUTE_SPEAKER -> "Speaker"
    CallAudioState.ROUTE_BLUETOOTH -> bluetoothName?.takeIf { it.isNotBlank() } ?: "Bluetooth"
    CallAudioState.ROUTE_WIRED_HEADSET -> "Headphones"
    CallAudioState.ROUTE_EARPIECE -> "Phone"
    else -> "Audio"
}

fun callAudioRouteIcon(route: Int): ImageVector = when (route) {
    CallAudioState.ROUTE_SPEAKER -> Icons.AutoMirrored.Filled.VolumeUp
    CallAudioState.ROUTE_BLUETOOTH -> Icons.Filled.Bluetooth
    CallAudioState.ROUTE_WIRED_HEADSET -> Icons.Filled.Headphones
    CallAudioState.ROUTE_EARPIECE -> Icons.Filled.Phone
    else -> Icons.AutoMirrored.Filled.VolumeUp
}

fun isEarpieceAudioRoute(route: Int): Boolean = route == CallAudioState.ROUTE_EARPIECE
