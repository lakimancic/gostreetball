package com.example.gostreetball.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gostreetball.data.model.BoardType
import com.example.gostreetball.data.model.CourtType
import com.example.gostreetball.data.repo.CourtRepository
import com.example.gostreetball.location.LocationManager
import com.example.gostreetball.utils.imageBitmapToByteArray
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddUiState (
    val name: String = "",
    val type: CourtType? = null,
    val boardType: BoardType? = null,
    val image: ImageBitmap? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class AddCourtViewModel @Inject constructor(
    private val courtRepository: CourtRepository,
    private val locationManager: LocationManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddUiState())
    val uiState: StateFlow<AddUiState> = _uiState.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack: SharedFlow<Unit> = _navigateBack

    private val _currentGeoPoint = MutableStateFlow<GeoPoint?>(null)
    private val currentGeoPoint: StateFlow<GeoPoint?> = _currentGeoPoint

    init {
        viewModelScope.launch {
            locationManager.currentLocation.collect { latLng ->
                _currentGeoPoint.value = latLng?.let { GeoPoint(it.latitude, it.longitude) }
            }
        }
    }

    fun updateName(name: String) = _uiState.update {
        it.copy(
            name = name
        )
    }

    fun updateType(type: CourtType?) = _uiState.update {
        it.copy(
            type = type
        )
    }

    fun updateBoardType(type: BoardType?) = _uiState.update {
        it.copy(
            boardType = type
        )
    }

    fun updateImage(bitmap: Bitmap?) = _uiState.update {
        it.copy(
            image = bitmap?.asImageBitmap()
        )
    }

    fun addCourt() {
        viewModelScope.launch {
            val type = uiState.value.type
            val boardType = uiState.value.boardType
            val image = uiState.value.image
            val geoPoint = currentGeoPoint.value
            _uiState.update { it.copy(errorMessage = null, isLoading = true) }

            if (type == null) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Please select a court type",
                        isLoading = false
                    )
                }
                return@launch
            }

            if (boardType == null) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Please select a board type",
                        isLoading = false
                    )
                }
                return@launch
            }

            if (image == null) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Profile picture is required",
                        isLoading = false
                    )
                }
                return@launch
            }

            if (geoPoint == null) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Unable to get current geo position",
                        isLoading = false
                    )
                }
                return@launch
            }

            val result = courtRepository.addCourt(uiState.value.name, type, boardType, imageBitmapToByteArray(image), geoPoint)

            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                _navigateBack.emit(Unit)
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "Failed to add court",
                        isLoading = false
                    )
                }
            }
        }
    }
}