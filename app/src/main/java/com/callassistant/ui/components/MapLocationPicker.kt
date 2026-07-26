package com.callassistant.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

private const val DEFAULT_ZOOM = 15.0
private val DEFAULT_POINT = GeoPoint(23.8103, 90.4125) // Dhaka fallback

@Composable
fun MapLocationPickerDialog(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onConfirm: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var selectedPoint by remember {
        mutableStateOf(
            if (initialLatitude != null && initialLongitude != null) {
                GeoPoint(initialLatitude, initialLongitude)
            } else {
                DEFAULT_POINT
            }
        )
    }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var markerRef by remember { mutableStateOf<Marker?>(null) }
    var isLocating by remember { mutableStateOf(false) }

    DisposableEffect(mapViewRef) {
        mapViewRef?.onResume()
        onDispose {
            mapViewRef?.onPause()
            mapViewRef?.onDetach()
        }
    }

    fun moveMarker(point: GeoPoint, recenter: Boolean = true) {
        selectedPoint = point
        markerRef?.position = point
        if (recenter) {
            mapViewRef?.controller?.animateTo(point)
        }
        mapViewRef?.invalidate()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchCurrentLocation(context) { location ->
                isLocating = false
                if (location != null) {
                    moveMarker(GeoPoint(location.latitude, location.longitude))
                }
            }
        } else {
            isLocating = false
        }
    }

    fun useCurrentLocation() {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            isLocating = true
            fetchCurrentLocation(context) { location ->
                isLocating = false
                if (location != null) {
                    moveMarker(GeoPoint(location.latitude, location.longitude))
                }
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Pick location",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    "Tap the map to drop a pin, or use your current position.",
                    style = MaterialTheme.typography.bodySmall
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(400.dp),
                        factory = { ctx ->
                            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", 0))
                            Configuration.getInstance().userAgentValue = ctx.packageName
                            val base = File(ctx.cacheDir, "osmdroid").apply { mkdirs() }
                            val cache = File(base, "tiles").apply { mkdirs() }
                            Configuration.getInstance().osmdroidBasePath = base
                            Configuration.getInstance().osmdroidTileCache = cache

                            val markerBitmap = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(markerBitmap)
                            val paint = Paint().apply {
                                color = Color.parseColor("#E53935")
                                isAntiAlias = true
                                style = Paint.Style.FILL
                            }
                            canvas.drawCircle(20f, 20f, 18f, paint)
                            val markerIcon = BitmapDrawable(ctx.resources, markerBitmap)

                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(DEFAULT_ZOOM)
                                controller.setCenter(selectedPoint)

                                val marker = Marker(this).apply {
                                    position = selectedPoint
                                    icon = markerIcon
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                }
                                overlays.add(marker)
                                markerRef = marker
                                mapViewRef = this

                                val tapOverlay = object : org.osmdroid.views.overlay.Overlay() {
                                    override fun onSingleTapConfirmed(
                                        e: android.view.MotionEvent,
                                        mapView: MapView
                                    ): Boolean {
                                        val projection = mapView.projection
                                        val geoPoint = projection.fromPixels(
                                            e.x.toInt(), e.y.toInt()
                                        ) as? GeoPoint ?: return false
                                        selectedPoint = geoPoint
                                        marker.position = geoPoint
                                        mapView.invalidate()
                                        return true
                                    }
                                }
                                overlays.add(tapOverlay)
                            }
                        },
                        update = { }
                    )
                    FloatingActionButton(
                        onClick = { useCurrentLocation() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = "Use current location")
                        }
                    }
                }
                Text(
                    "Lat: ${"%.6f".format(selectedPoint.latitude)}  Lng: ${"%.6f".format(selectedPoint.longitude)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = {
                        onConfirm(selectedPoint.latitude, selectedPoint.longitude)
                    }) { Text("Use this location") }
                }
            }
        }
    }
}

private fun fetchCurrentLocation(
    context: android.content.Context,
    onResult: (Location?) -> Unit
) {
    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager == null) {
        onResult(null)
        return
    }

    val hasFine = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasFine && !hasCoarse) {
        onResult(null)
        return
    }

    val providers = locationManager.getProviders(true)
    val lastKnown = providers
        .mapNotNull { provider ->
            try {
                locationManager.getLastKnownLocation(provider)
            } catch (_: SecurityException) {
                null
            }
        }
        .maxByOrNull { it.time }

    if (lastKnown != null) {
        onResult(lastKnown)
        return
    }

    val provider = when {
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> null
    }

    if (provider == null) {
        onResult(null)
        return
    }

    try {
        locationManager.requestSingleUpdate(provider, { location ->
            onResult(location)
        }, Looper.getMainLooper())
    } catch (_: SecurityException) {
        onResult(null)
    }
}
