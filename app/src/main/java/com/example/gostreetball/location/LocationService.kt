package com.example.gostreetball.location

import kotlinx.coroutines.cancel
import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.gostreetball.data.local.AppPreferences
import com.example.gostreetball.data.repo.UserRepository
import com.example.gostreetball.utils.NotificationUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {
    private lateinit var notifier: NotificationUtils
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastNotifiedIds: Set<String> = emptySet()
    private var checkRadius: Double = 100.0

    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var fusedLocationClient: FusedLocationProviderClient
    @Inject lateinit var preferences: AppPreferences

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val NEARBY_NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        notifier = NotificationUtils(this)

        serviceScope.launch {
            preferences.checkRadiusMeters.collectLatest {
                checkRadius = it.toDouble()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notifier.createPersistentNotification(
            title = "Location tracking",
            message = "Location tracking service is running.",
        )

        startForeground(1, notification)

        runnable = object : Runnable {
            override fun run() {
                checkLocationAndNotify()
                handler.postDelayed(this, 30_000L)
            }
        }
        handler.post(runnable)

        return START_STICKY
    }

    private fun checkLocationAndNotify() {
        val hasPermissions = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermissions)
            return

        val tokenSource = CancellationTokenSource()
        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
            .addOnSuccessListener { current ->
                if (current != null) {
                    triggerLocationCheck(current.latitude, current.longitude)
                } else {
                    Log.w("LocationService", "Could not obtain current location.")
                }
            }
            .addOnFailureListener { e -> Log.e("LocationService", "getCurrentLocation failed", e) }
    }

    private fun triggerLocationCheck(lat: Double, lng: Double) {
        serviceScope.launch {
            val result = userRepository.updateLocation(LatLng(lat, lng))

            result.onSuccess { timestamp ->
                Log.d("LocationService", "Location updated at $timestamp")
            }.onFailure { e ->
                Log.e("LocationService", "Failed to update location", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
        serviceScope.cancel()
        Log.d("LocationService", "Service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}