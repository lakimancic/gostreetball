package com.example.gostreetball.ui

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gostreetball.data.model.BoardType
import com.example.gostreetball.data.model.Court
import com.example.gostreetball.data.model.CourtType
import com.example.gostreetball.data.model.Game
import com.example.gostreetball.data.repo.CourtRepository
import com.example.gostreetball.data.repo.GameRepository
import com.example.gostreetball.data.repo.UserRepository
import com.example.gostreetball.location.LocationManager
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SCourtUiState (
    val court: Court? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasReviewed: Boolean = false,
    val canJoinCourt: Boolean = false,
    val games: List<Game> = emptyList()
)

@HiltViewModel
class CourtViewModel @Inject constructor(
    private val locationManager: LocationManager,
    private val courtRepository: CourtRepository,
    private val userRepository: UserRepository,
    private val gameRepository: GameRepository
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

    fun joinCourt() {
        viewModelScope.launch {
            val courtId = _uiState.value.court?.id ?: return@launch

            _uiState.update { it.copy(isLoading = true) }
            val result = userRepository.joinCourt(courtId)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
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

    fun fetchCourtGames(courtId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val games = gameRepository.getCourtGames(courtId)
                _uiState.update { currentState ->
                    currentState.copy(
                        games = games,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }
}