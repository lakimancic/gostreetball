package com.example.gostreetball.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.example.gostreetball.location.LocationManager
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationManager: LocationManager
) : ViewModel() {
    val currentLocation: StateFlow<LatLng?> = locationManager.currentLocation
    val isLocationAvailable: StateFlow<Boolean> = locationManager.isLocationAvailable
    val locationPermissionGranted: StateFlow<Boolean> = locationManager.locationPermissionGranted

    fun requestLocationPermissions(): Array<String> {
        return locationManager.requestPermissions()
    }

    fun onLocationPermissionsGranted() {
        locationManager.onPermissionsGranted()
    }

    override fun onCleared() {
        super.onCleared()
        locationManager.stopLocationUpdates()
    }
}