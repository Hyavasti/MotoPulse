package com.example.motorapp.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.motorapp.data.ChatRepository
import com.example.motorapp.data.ConvoyRiderLocation
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.catch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

private val LeafGreen = Color(0xFF16A34A)
private val DarkSlate = Color(0xFF0F172A)
private val GreenTintBg = Color(0xFFF0FDF4)
private val BorderGreen = Color(0xFFBBF7D0)

private val FREE_OSM_TILES = XYTileSource(
    "CyclOSM",
    0, 18, 256, ".png",
    arrayOf(
        "https://a.tile-cyclosm.openstreetmap.fr/cyclosm/",
        "https://b.tile-cyclosm.openstreetmap.fr/cyclosm/",
        "https://c.tile-cyclosm.openstreetmap.fr/cyclosm/"
    )
)

@SuppressLint("MissingPermission")
@Composable
fun ConvoyMapScreen(
    rideId: String,
    destinationName: String,
    repository: ChatRepository = remember { ChatRepository() },
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var joinedRiders by remember { mutableStateOf<List<ConvoyRiderLocation>>(emptyList()) }
    val myUserId = repository.currentUserId
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    var lockedRiderId by remember { mutableStateOf<String?>(myUserId) }

    // Immediate Hardware GPS Center (0-latency initial centering)
    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    mapViewRef?.controller?.setCenter(GeoPoint(loc.latitude, loc.longitude))
                }
            }
        }
    }

    // Stream live locations of joined riders from Supabase
    LaunchedEffect(rideId) {
        repository.getJoinedRidersStream(rideId)
            .catch { /* ignore network drops */ }
            .collect { list ->
                joinedRiders = list
                mapViewRef?.let { map ->
                    updateMarkers(map, list, myUserId) { clickedUserId ->
                        lockedRiderId = clickedUserId
                    }

                    if (lockedRiderId != null) {
                        val target = list.firstOrNull { it.userId == lockedRiderId }
                        if (target?.latitude != null && target.longitude != null) {
                            map.controller.animateTo(GeoPoint(target.latitude, target.longitude))
                        }
                    }
                }
            }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef?.onDetach()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val osmConfig = Configuration.getInstance()
                osmConfig.userAgentValue = "${ctx.packageName}/1.0 (Android; MotorApp Convoy)"

                osmConfig.tileDownloadThreads = 8.toShort()
                osmConfig.tileFileSystemThreads = 8.toShort()

                val basePath = File(ctx.cacheDir, "osmdroid")
                val tilePath = File(basePath, "tiles")
                osmConfig.osmdroidBasePath = basePath
                osmConfig.osmdroidTileCache = tilePath
                osmConfig.tileFileSystemCacheMaxBytes = 300L * 1024L * 1024L
                osmConfig.tileFileSystemCacheTrimBytes = 250L * 1024L * 1024L

                MapView(ctx).apply {
                    setTileSource(FREE_OSM_TILES)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(16.0)

                    // Check last known location instantly on View inflation
                    val hasPerm = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        LocationServices.getFusedLocationProviderClient(ctx).lastLocation.addOnSuccessListener { loc ->
                            if (loc != null) {
                                controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
                            }
                        }
                    } else {
                        controller.setCenter(GeoPoint(14.1153, 120.9621))
                    }

                    setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_MOVE && event.pointerCount == 1) {
                            lockedRiderId = null
                        }
                        false
                    }

                    mapViewRef = this
                }
            },
            update = { map ->
                mapViewRef = map
            }
        )

        // Top Header Bar
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, BorderGreen),
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = GreenTintBg,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, BorderGreen),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { onNavigateBack() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkSlate,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = destinationName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate
                    )
                    Text(
                        text = "${joinedRiders.size} Active Rider${if (joinedRiders.size != 1) "s" else ""}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LeafGreen
                    )
                }

                AnimatedVisibility(
                    visible = lockedRiderId != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = GreenTintBg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderGreen),
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = LeafGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (lockedRiderId == myUserId) "Locked (You)" else "Locked",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LeafGreen
                            )
                        }
                    }
                }
            }
        }

        // Floating Controls (Zoom + Quick Center)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { mapViewRef?.controller?.zoomIn() },
                containerColor = Color.White,
                contentColor = DarkSlate,
                shape = CircleShape,
                modifier = Modifier.size(42.dp),
                elevation = FloatingActionButtonDefaults.elevation(3.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
            }

            SmallFloatingActionButton(
                onClick = { mapViewRef?.controller?.zoomOut() },
                containerColor = Color.White,
                contentColor = DarkSlate,
                shape = CircleShape,
                modifier = Modifier.size(42.dp),
                elevation = FloatingActionButtonDefaults.elevation(3.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            FloatingActionButton(
                onClick = {
                    lockedRiderId = myUserId
                    val self = joinedRiders.firstOrNull { it.userId == myUserId }
                    if (self?.latitude != null && self.longitude != null) {
                        mapViewRef?.controller?.animateTo(GeoPoint(self.latitude, self.longitude))
                    }
                },
                containerColor = if (lockedRiderId == myUserId) LeafGreen else Color.White,
                contentColor = if (lockedRiderId == myUserId) Color.White else LeafGreen,
                shape = CircleShape,
                modifier = Modifier.size(46.dp),
                elevation = FloatingActionButtonDefaults.elevation(3.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Center on me", modifier = Modifier.size(22.dp))
            }
        }

        // Bottom Rider Focus Bubbles
        if (joinedRiders.isNotEmpty()) {
            Surface(
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 22.dp)
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(joinedRiders, key = { it.userId }) { rider ->
                        val isMe = rider.userId == myUserId
                        val isLocked = lockedRiderId == rider.userId
                        val displayName = if (isMe) "You" else "Rider ${rider.userId.take(4)}"

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (isLocked) (if (isMe) LeafGreen else Color(0xFF2563EB)) else Color.White,
                            border = BorderStroke(
                                1.5.dp,
                                if (isLocked) Color.Transparent else if (isMe) LeafGreen else BorderGreen
                            ),
                            shadowElevation = if (isLocked) 6.dp else 2.dp,
                            modifier = Modifier
                                .clickable {
                                    lockedRiderId = rider.userId
                                    if (rider.latitude != null && rider.longitude != null) {
                                        mapViewRef?.controller?.animateTo(
                                            GeoPoint(rider.latitude, rider.longitude)
                                        )
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isLocked) Color.White.copy(alpha = 0.25f)
                                            else if (isMe) GreenTintBg
                                            else Color(0xFFEFF6FF)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isLocked) Color.White else if (isMe) LeafGreen else Color(0xFF2563EB),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column {
                                    Text(
                                        text = displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isLocked) Color.White else DarkSlate,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = null,
                                            tint = if (isLocked) Color.White.copy(alpha = 0.85f) else LeafGreen,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${rider.speedKmh.toInt()} km/h",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isLocked) Color.White.copy(alpha = 0.85f) else DarkSlate.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun updateMarkers(
    mapView: MapView,
    riders: List<ConvoyRiderLocation>,
    myUserId: String?,
    onMarkerClick: (userId: String) -> Unit
) {
    mapView.overlays.clear()
    val ctx = mapView.context

    riders.forEach { rider ->
        val lat = rider.latitude
        val lng = rider.longitude
        if (lat != null && lng != null) {
            val isMe = rider.userId == myUserId
            val pinColor = if (isMe) LeafGreen.toArgb() else android.graphics.Color.parseColor("#2563EB")

            val marker = Marker(mapView).apply {
                position = GeoPoint(lat, lng)
                title = if (isMe) "You" else "Convoy Rider"
                snippet = "${rider.speedKmh.toInt()} km/h"
                icon = createCircleMarkerDrawable(ctx, pinColor)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setOnMarkerClickListener { _, _ ->
                    onMarkerClick(rider.userId)
                    true
                }
            }
            mapView.overlays.add(marker)
        }
    }
    mapView.invalidate()
}

private fun createCircleMarkerDrawable(context: android.content.Context, colorArgb: Int): BitmapDrawable {
    val sizePx = 48
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 2f, whitePaint)

    val colorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorArgb
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 6f, colorPaint)

    return BitmapDrawable(context.resources, bitmap)
}