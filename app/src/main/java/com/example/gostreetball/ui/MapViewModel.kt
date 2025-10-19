package com.example.gostreetball.ui

import androidx.lifecycle.ViewModel
import com.example.gostreetball.location.LocationRepository
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {
    val currentLocation: StateFlow<LatLng?> = locationRepository.currentLocation
    val isLocationAvailable: StateFlow<Boolean> = locationRepository.isLocationAvailable
    val locationPermissionGranted: StateFlow<Boolean> = locationRepository.locationPermissionGranted

    fun requestLocationPermissions(): Array<String> {
        return locationRepository.requestPermissions()
    }

    fun onLocationPermissionsGranted() {
        locationRepository.onPermissionsGranted()
    }

    override fun onCleared() {
        super.onCleared()
        locationRepository.stopLocationUpdates()
    }
}