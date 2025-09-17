package com.example.gostreetball.ui

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gostreetball.data.model.BoardType
import com.example.gostreetball.data.model.Court
import com.example.gostreetball.data.model.CourtType
import com.example.gostreetball.data.repo.CourtRepository
import com.example.gostreetball.location.LocationManager
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SCourtUiState (
    val court: Court? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasReviewed: Boolean = false,
    val canJoinCourt: Boolean = false
)

@HiltViewModel
class CourtViewModel @Inject constructor(
    private val locationManager: LocationManager,
    private val courtRepository: CourtRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SCourtUiState())
    val uiState: StateFlow<SCourtUiState> = _uiState.asStateFlow()

    fun fetchCourtById(courtId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            val result = courtRepository.getCourt(courtId)

            result.fold(
                onSuccess = { (court, hasReviewed) ->
                    val userLocation = locationManager.currentLocation.value
                    val canJoin = if (userLocation != null && court.location != null) {
                        isUserNearCourt(
                            userLocation.latitude,
                            userLocation.longitude,
                            court.location.latitude,
                            court.location.longitude
                        )
                    } else false

                    _uiState.value = _uiState.value.copy(
                        court = court,
                        isLoading = false,
                        canJoinCourt = canJoin,
                        hasReviewed = hasReviewed
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            )
        }
    }

    private fun isUserNearCourt(
        userLat: Double,
        userLng: Double,
        courtLat: Double,
        courtLng: Double,
        radiusMeters: Double = 100.0
    ): Boolean {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(userLat, userLng, courtLat, courtLng, results)
        return results[0] <= radiusMeters
    }
}