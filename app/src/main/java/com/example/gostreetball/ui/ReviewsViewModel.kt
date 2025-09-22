package com.example.gostreetball.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gostreetball.data.model.Review
import com.example.gostreetball.data.model.User
import com.example.gostreetball.data.repo.CourtRepository
import com.example.gostreetball.data.repo.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewsUiState (
    val reviews: List<Pair<Review, User?>> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    private val courtRepository: CourtRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReviewsUiState())
    val uiState: StateFlow<ReviewsUiState> = _uiState.asStateFlow()

    fun fetchReviews(itemId: String, isForCourt: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val reviews = if (isForCourt) {
                    courtRepository.getReviews(itemId).getOrThrow()
                } else {
                    userRepository.getReviews(itemId).getOrThrow()
                }

                val userIds = reviews.map { it.userId }.distinct()
                val users = userRepository.getUsersForIds(userIds).getOrThrow()

                val usersById = users.associateBy { it.uid }

                val reviewWithUser = reviews.map { review ->
                    review to usersById[review.userId]
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    reviews = reviewWithUser
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}