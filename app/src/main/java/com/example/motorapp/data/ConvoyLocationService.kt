package com.example.motorapp.data

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.motorapp.MainActivity
import com.example.motorapp.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ConvoyLocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val repository = ChatRepository()

    private var activeRideId: String? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val rideId = activeRideId ?: return
                    sendLocationUpdate(rideId, location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val rideId = intent.getStringExtra(EXTRA_RIDE_ID)
                val destinationName = intent.getStringExtra(EXTRA_DESTINATION) ?: "Convoy Ride"
                if (rideId != null) {
                    activeRideId = rideId
                    startForeground(NOTIFICATION_ID, createNotification(destinationName))
                    startLocationTracking()
                }
            }
            ACTION_STOP -> {
                stopLocationTracking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationTracking() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(3000L)
            .setMinUpdateDistanceMeters(2f)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun sendLocationUpdate(rideId: String, location: Location) {
        serviceScope.launch {
            repository.updateRiderLocation(
                rideId = rideId,
                lat = location.latitude,
                lng = location.longitude,
                heading = location.bearing,
                speedKmh = (location.speed * 3.6f) // convert m/s to km/h
            )
        }
    }

    private fun createNotification(destination: String): Notification {
        val channelId = "motopulse_convoy_tracking"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Convoy Ride Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active GPS broadcasting during convoy rides"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("MotoPulse Convoy Active")
            .setContentText("Sharing location for: $destination")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationTracking()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "ACTION_START_CONVOY_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_CONVOY_TRACKING"
        const val EXTRA_RIDE_ID = "EXTRA_RIDE_ID"
        const val EXTRA_DESTINATION = "EXTRA_DESTINATION"
        private const val NOTIFICATION_ID = 2001

        fun startService(context: Context, rideId: String, destinationName: String) {
            val intent = Intent(context, ConvoyLocationService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RIDE_ID, rideId)
                putExtra(EXTRA_DESTINATION, destinationName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ConvoyLocationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}