package com.example.motorapp.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.motorapp.services.RideWeatherMonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

private val LeafGreen = Color(0xFF16A34A)
private val LightScreenBg = Color(0xFFF8FAFC)
private val CardBg = Color(0xFFFFFFFF)
private val TextDark = Color(0xFF0F172A)
private val TextSubtle = Color(0xFF64748B)
private val CardBorderColor = Color(0xFFE2E8F0)

data class DestinationWeather(
    val temperatureC: Double,
    val weatherCode: Int,
    val precipitationChance: Int,
    val windSpeedKph: Double,
    val conditionDescription: String,
    val isRainLikely: Boolean
)

data class WeatherPreset(
    val name: String,
    val lat: Double,
    val lng: Double
)

private data class QuickPitstopCategory(
    val title: String,
    val subtitle: String,
    val query: String,
    val icon: ImageVector,
    val accentColor: Color
)

suspend fun fetchDestinationWeather(lat: Double, lng: Double): DestinationWeather? = withContext(Dispatchers.IO) {
    try {
        val endpoint = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lng&current=temperature_2m,weather_code,wind_speed_10m&hourly=precipitation_probability&forecast_days=1"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4000
            readTimeout = 4000
        }

        if (connection.responseCode == 200) {
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            val current = json.getJSONObject("current")
            val hourly = json.getJSONObject("hourly")

            val temp = current.getDouble("temperature_2m")
            val code = current.getInt("weather_code")
            val wind = current.getDouble("wind_speed_10m")

            val rainProbs = hourly.getJSONArray("precipitation_probability")
            val peakRainChance = (0 until minOf(6, rainProbs.length())).maxOf { rainProbs.getInt(it) }

            val (desc, isRain) = parseWeatherCode(code)

            DestinationWeather(
                temperatureC = temp,
                weatherCode = code,
                precipitationChance = peakRainChance,
                windSpeedKph = wind,
                conditionDescription = desc,
                isRainLikely = isRain || peakRainChance >= 40
            )
        } else null
    } catch (e: Exception) {
        null
    }
}

private fun parseWeatherCode(code: Int): Pair<String, Boolean> {
    return when (code) {
        0 -> "Clear Sky ☀️" to false
        1, 2, 3 -> "Partly Cloudy ⛅" to false
        45, 48 -> "Foggy 🌫️" to false
        51, 53, 55 -> "Light Drizzle 🌦️" to true
        61, 63, 65 -> "Rainy 🌧️" to true
        80, 81, 82 -> "Heavy Showers ⛈️" to true
        95, 96, 99 -> "Thunderstorm ⚡" to true
        else -> "Cloudy ☁️" to false
    }
}

fun launchGoogleMapsQuery(context: Context, query: String) {
    val encodedQuery = Uri.encode(query.trim())
    val mapUri = Uri.parse("geo:0,0?q=$encodedQuery")
    val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
        setPackage("com.google.android.apps.maps")
    }

    if (mapIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(mapIntent)
    } else {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedQuery")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        } catch (e: Exception) {
            Toast.makeText(context, "No map application available", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun RiderRouteLauncherScreen(
    onBackToCockpit: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var currentWeather by remember { mutableStateOf<DestinationWeather?>(null) }
    var weatherAreaName by remember { mutableStateOf("Your Location") }
    var isCheckingWeather by remember { mutableStateOf(false) }

    var isRideCompanionActive by remember {
        mutableStateOf(RideWeatherMonitorService.isServiceRunning)
    }

    val locationManager = remember {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    val weatherPresets = listOf(
        WeatherPreset("Current GPS", 0.0, 0.0),
        WeatherPreset("Tagaytay", 14.1153, 120.9621),
        WeatherPreset("Marilaque", 14.6186, 121.3653),
        WeatherPreset("Kaybiang Tunnel", 14.2818, 120.5978),
        WeatherPreset("Batangas City", 13.7565, 121.0583),
        WeatherPreset("Metro Manila", 14.5995, 120.9842)
    )

    val pitstopActions = listOf(
        QuickPitstopCategory(
            title = "Gas Stations",
            subtitle = "Find nearest Shell, Petron, Caltex",
            query = "Gas station near me",
            icon = Icons.Default.LocalGasStation,
            accentColor = Color(0xFFEF4444)
        ),
        QuickPitstopCategory(
            title = "Vulcanizing Shops",
            subtitle = "Tire patch & emergency air",
            query = "Vulcanizing shop near me",
            icon = Icons.Default.Build,
            accentColor = Color(0xFFF59E0B)
        ),
        QuickPitstopCategory(
            title = "Motorcycle Repair & Parts",
            subtitle = "Talyer, mechanics & moto supplies",
            query = "Motorcycle repair shop near me",
            icon = Icons.Default.TwoWheeler,
            accentColor = LeafGreen
        ),
        QuickPitstopCategory(
            title = "Convenience & Hydration",
            subtitle = "7-Eleven, Lawson, Alfamart stops",
            query = "Convenience store near me",
            icon = Icons.Default.ShoppingBag,
            accentColor = Color(0xFF3B82F6)
        ),
        QuickPitstopCategory(
            title = "Rider Cafés & Overlooks",
            subtitle = "Popular coffee pitstops nearby",
            query = "Coffee shop overlooking near me",
            icon = Icons.Default.Coffee,
            accentColor = Color(0xFF8B5CF6)
        )
    )

    @SuppressLint("MissingPermission")
    fun loadCurrentLocationWeather() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return

        isCheckingWeather = true
        val location: Location? =
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (location != null) {
            coroutineScope.launch {
                currentWeather = fetchDestinationWeather(location.latitude, location.longitude)
                weatherAreaName = "Your Departure Area"
                isCheckingWeather = false
            }
        } else {
            isCheckingWeather = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            loadCurrentLocationWeather()
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(context, RideWeatherMonitorService::class.java).apply {
                action = RideWeatherMonitorService.ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
            isRideCompanionActive = true
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            loadCurrentLocationWeather()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightScreenBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = CardBg,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { onBackToCockpit() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "Ride Navigation & Stops",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "Live weather & instant pitstop launcher",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = LeafGreen
                    )
                }
            }

            // Ride Companion / Guard Toggle Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isRideCompanionActive) LeafGreen.copy(alpha = 0.08f) else CardBg,
                border = BorderStroke(1.dp, if (isRideCompanionActive) LeafGreen else CardBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isRideCompanionActive) LeafGreen else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = if (isRideCompanionActive) Color.White else TextSubtle,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRideCompanionActive) "Ride Guard Active" else "En-Route Rain Alert",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = if (isRideCompanionActive)
                                "Alerts you if rain risk spikes along your route"
                            else
                                "Monitors weather under your wheels while using Maps",
                            fontSize = 12.sp,
                            color = TextSubtle
                        )
                    }

                    Switch(
                        checked = isRideCompanionActive,
                        onCheckedChange = { enable ->
                            if (enable) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val intent = Intent(context, RideWeatherMonitorService::class.java).apply {
                                        action = RideWeatherMonitorService.ACTION_START
                                    }
                                    ContextCompat.startForegroundService(context, intent)
                                    isRideCompanionActive = true
                                }
                            } else {
                                val intent = Intent(context, RideWeatherMonitorService::class.java).apply {
                                    action = RideWeatherMonitorService.ACTION_STOP
                                }
                                context.startService(intent)
                                isRideCompanionActive = false
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = LeafGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Weather Region Selection
            Text(
                text = "ROUTE WEATHER ADVISORY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSubtle
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(weatherPresets) { preset ->
                    val isGpsChip = preset.name == "Current GPS"
                    val isSelected = weatherAreaName.equals(preset.name, ignoreCase = true) ||
                            (isGpsChip && weatherAreaName == "Your Departure Area")

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) LeafGreen.copy(alpha = 0.12f) else CardBg,
                        border = BorderStroke(1.dp, if (isSelected) LeafGreen else CardBorderColor),
                        modifier = Modifier.clickable {
                            if (isGpsChip) {
                                loadCurrentLocationWeather()
                            } else {
                                coroutineScope.launch {
                                    isCheckingWeather = true
                                    currentWeather = fetchDestinationWeather(preset.lat, preset.lng)
                                    weatherAreaName = preset.name
                                    isCheckingWeather = false
                                }
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Icon(
                                imageVector = if (isGpsChip) Icons.Default.MyLocation else Icons.Default.Place,
                                contentDescription = null,
                                tint = if (isSelected) LeafGreen else TextSubtle,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = preset.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) LeafGreen else TextDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Weather Card
            if (isCheckingWeather) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = LeafGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Updating weather advisory...",
                        fontSize = 12.sp,
                        color = TextSubtle
                    )
                }
            }

            AnimatedVisibility(
                visible = currentWeather != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                currentWeather?.let { weather ->
                    val isRain = weather.isRainLikely
                    val cardBg = if (isRain) Color(0xFFFEF2F2) else Color(0xFFF0FDF4)
                    val borderColor = if (isRain) Color(0xFFFCA5A5) else Color(0xFF86EFAC)
                    val rainTextColor = if (isRain) Color(0xFFDC2626) else LeafGreen

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cloud,
                                        contentDescription = null,
                                        tint = if (isRain) Color(0xFFDC2626) else LeafGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = weatherAreaName.uppercase(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                }
                                Text(
                                    text = weather.conditionDescription,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${weather.temperatureC.roundToInt()}°C",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextDark
                                )

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Rain Probability: ${weather.precipitationChance}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = rainTextColor
                                    )
                                    Text(
                                        text = "Wind: ${weather.windSpeedKph.roundToInt()} km/h",
                                        fontSize = 11.sp,
                                        color = TextSubtle
                                    )
                                }
                            }

                            if (isRain) {
                                Text(
                                    text = "⚠️ Rain alert: Pack a raincoat before hitting the road!",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Roadside Utilities Header
            Text(
                text = "INSTANT ROADSIDE PITSTOPS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSubtle
            )
            Text(
                text = "Launches Google Maps with real-time POI search",
                fontSize = 12.sp,
                color = TextSubtle,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Pitstop Action Cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                pitstopActions.forEach { action ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CardBg,
                        border = BorderStroke(1.dp, CardBorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { launchGoogleMapsQuery(context, action.query) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(action.accentColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = null,
                                    tint = action.accentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = action.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = action.subtitle,
                                    fontSize = 12.sp,
                                    color = TextSubtle
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = "Launch",
                                tint = LeafGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Open Full Google Maps App Tile
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = LeafGreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { launchGoogleMapsQuery(context, "") }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Open Full Google Maps",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}