package com.example.gostreetball.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gostreetball.data.model.CourtType
import com.example.gostreetball.data.repo.CourtRepository
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
    val image: ImageBitmap? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class AddCourtViewModel @Inject constructor(
    private val courtRepository: CourtRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddUiState())
    val uiState: StateFlow<AddUiState> = _uiState.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack: SharedFlow<Unit> = _navigateBack

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

    fun updateImage(bitmap: Bitmap?) = _uiState.update {
        it.copy(
            image = bitmap?.asImageBitmap()
        )
    }

    fun addCourt() {
        viewModelScope.launch {
            val type = uiState.value.type
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

            val result = courtRepository.addCourt(uiState.value.name, type)

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