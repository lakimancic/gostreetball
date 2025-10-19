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
import androidx.core.app.ActivityCompat
import com.example.gostreetball.data.local.AppPreferences
import com.example.gostreetball.data.repo.CourtRepository
import com.example.gostreetball.data.repo.UserRepository
import com.example.gostreetball.utils.NotificationUtils
import com.google.android.gms.location.FusedLocationProviderClient
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
    private var lastNotifiedCourtIds: Set<String> = emptySet()
    private var lastNotifiedUserIds: Set<String> = emptySet()
    private var lastUpdateTime: Long = 0
    private var checkRadius: Double = 100.0

    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var courtRepository: CourtRepository
    @Inject lateinit var locationRepository: LocationRepository
    @Inject lateinit var preferences: AppPreferences

    companion object {
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        notifier = NotificationUtils(this)
        isRunning = true

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
                handler.postDelayed(this, 5_000L)
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

        val location = locationRepository.currentLocation.value
        if (location != null) {
            triggerLocationCheck(location.latitude, location.longitude)
        }
    }

    private fun triggerLocationCheck(lat: Double, lng: Double) {
        serviceScope.launch {
            val result = userRepository.updateLocation(LatLng(lat, lng))

            result.onSuccess { timestamp ->
                Log.d("LocationService", "Location updated at $timestamp")
            }.onFailure { e ->
                Log.e("LocationService", "Failed to update location", e)
            }

            val newCourtsNames = mutableListOf<String>()
            val newUsersNames = mutableListOf<String>()

            val courtsResult = courtRepository.getCourtsInRadius(lat, lng, checkRadius)
            val currentCourtIds = mutableSetOf<String>()
            courtsResult.onSuccess { courts ->
                currentCourtIds.addAll(courts.map { it.id })
                val newCourts = courts.filter { it.id !in lastNotifiedCourtIds }
                newCourtsNames.addAll(newCourts.map { it.name })
            }.onFailure { e ->
                Log.e("LocationService", "Failed to fetch nearby courts", e)
            }

            val usersResult = userRepository.getUsersInRadius(lat, lng, checkRadius)
            val currentUserIds = mutableSetOf<String>()
            usersResult.onSuccess { users ->
                currentUserIds.addAll(users.map { it.uid })
                val newUsers = users.filter { it.uid !in lastNotifiedUserIds }
                newUsersNames.addAll(newUsers.map { it.username })
            }.onFailure { e ->
                Log.e("LocationService", "Failed to fetch nearby users", e)
            }

            if (newCourtsNames.isNotEmpty() || newUsersNames.isNotEmpty()) {
                val messageBuilder = StringBuilder()
                if (newCourtsNames.isNotEmpty()) {
                    messageBuilder.append("New Courts Nearby:\n")
                    newCourtsNames.forEach { name -> messageBuilder.append("• $name\n") }
                }
                if (newUsersNames.isNotEmpty()) {
                    messageBuilder.append("New Players Nearby:\n")
                    newUsersNames.forEach { name -> messageBuilder.append("• $name\n") }
                }

                notifier.showNotification(
                    notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                    title = "Nearby Updates",
                    message = messageBuilder.toString()
                )
            }

            userRepository.clearCurrentUserCourtIfInvalid(currentCourtIds)

            lastNotifiedCourtIds = currentCourtIds
            lastNotifiedUserIds = currentUserIds
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(runnable)
        serviceScope.cancel()
        Log.d("LocationService", "Service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}