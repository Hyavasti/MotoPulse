package com.example.motorapp.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.motorapp.MainActivity
import com.example.motorapp.R
import com.example.motorapp.screens.fetchDestinationWeather
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RideWeatherMonitorService : Service(), LocationListener {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var locationManager: LocationManager
    private var lastCheckedTimestamp = 0L
    private var lastRainProbability = 0

    companion object {
        const val CHANNEL_ID = "motopulse_ride_guard"
        const val ALERT_CHANNEL_ID = "motopulse_weather_alerts"
        const val NOTIFICATION_ID = 1001
        const val ALERT_NOTIFICATION_ID = 1002

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

        var isServiceRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRideMonitor()
            ACTION_STOP -> stopRideMonitor()
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startRideMonitor() {
        isServiceRunning = true
        val notification = buildOngoingNotification("Monitoring weather along your route...")
        startForeground(NOTIFICATION_ID, notification)

        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            // Request location updates every 5 minutes or 2 kilometers
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5 * 60 * 1000L,
                2000f,
                this
            )
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                5 * 60 * 1000L,
                2000f,
                this
            )

            // Trigger immediate check on last known spot
            val lastLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            lastLoc?.let { checkWeather(it) }
        }
    }

    private fun stopRideMonitor() {
        isServiceRunning = false
        locationManager.removeUpdates(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onLocationChanged(location: Location) {
        val now = System.currentTimeMillis()
        // Throttle API requests: check at most once every 10 minutes
        if (now - lastCheckedTimestamp > 10 * 60 * 1000L) {
            checkWeather(location)
        }
    }

    private fun checkWeather(location: Location) {
        lastCheckedTimestamp = System.currentTimeMillis()
        serviceScope.launch {
            val weather = fetchDestinationWeather(location.latitude, location.longitude) ?: return@launch

            // If rain chance is 45%+ or current condition is rainy, sound an alert
            if (weather.isRainLikely && weather.precipitationChance >= 45) {
                if (weather.precipitationChance != lastRainProbability) {
                    lastRainProbability = weather.precipitationChance
                    sendRainAlertNotification(weather.precipitationChance, weather.conditionDescription)
                }
            }

            // Update ongoing status text
            val updateNotif = buildOngoingNotification("${weather.temperatureC.toInt()}°C • Rain risk: ${weather.precipitationChance}%")
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, updateNotif)
        }
    }

    private fun sendRainAlertNotification(rainChance: Int, condition: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alert = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Rain Alert on Route!")
            .setContentText("$condition detected ($rainChance% rain chance). Consider stopping for rain gear.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        manager.notify(ALERT_NOTIFICATION_ID, alert)
    }

    private fun buildOngoingNotification(statusText: String): android.app.Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("MotoPulse Ride Guard Active")
            .setContentText(statusText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val ongoingChannel = NotificationChannel(
                CHANNEL_ID,
                "Ride Guard Active Status",
                NotificationManager.IMPORTANCE_LOW
            )

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "En-Route Weather Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
            }

            manager.createNotificationChannel(ongoingChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        locationManager.removeUpdates(this)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}