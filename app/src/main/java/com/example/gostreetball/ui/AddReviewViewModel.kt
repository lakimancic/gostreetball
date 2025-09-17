package com.example.gostreetball.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gostreetball.data.repo.CourtRepository
import com.example.gostreetball.data.repo.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddReviewUiState (
    val rating: Int = 0,
    val reviewText: String = "",
    val error: String? = null,
    val isLoading: Boolean = false,
    val isUpdate: Boolean = false
)

@HiltViewModel
class AddReviewViewModel @Inject constructor(
    private val courtRepository: CourtRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddReviewUiState())
    val uiState: StateFlow<AddReviewUiState> = _uiState.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack: SharedFlow<Unit> = _navigateBack

    fun updateRating(rating: Int) {
        _uiState.value = _uiState.value.copy(rating = rating)
    }

    fun updateReviewText(text: String) {
        _uiState.value = _uiState.value.copy(reviewText = text)
    }

    fun fetchForUpdate(itemId: String, isForCourt: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isUpdate = false) }

            val result = if (isForCourt) {
                courtRepository.getReview(itemId)
            } else {
                userRepository.getReview(itemId)
            }

            result.onSuccess { review ->
                if (review != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            reviewText = review.text,
                            rating = review.stars,
                            isUpdate = true
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun addReview(itemId: String, isForCourt: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            if (uiState.value.rating == 0) {
                _uiState.update {
                    it.copy(
                        error = "Please select a stars",
                        isLoading = false
                    )
                }
                return@launch
            }

            if (uiState.value.reviewText.isBlank()) {
                _uiState.update {
                    it.copy(
                        error = "Please write some review",
                        isLoading = false
                    )
                }
                return@launch
            }

            val result = if (isForCourt) {
                courtRepository.addReview(
                    courtId = itemId,
                    stars = _uiState.value.rating,
                    text = _uiState.value.reviewText
                )
            } else {
                userRepository.addReview(
                    userId = itemId,
                    stars = _uiState.value.rating,
                    text = _uiState.value.reviewText
                )
            }

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false
                    )
                    _navigateBack.emit(Unit)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }
}