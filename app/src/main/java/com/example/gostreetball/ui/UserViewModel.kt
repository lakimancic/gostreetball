package com.example.gostreetball.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gostreetball.data.model.User
import com.example.gostreetball.data.repo.AuthRepository
import com.example.gostreetball.data.repo.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserUiState (
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasReviewed: Boolean = false,
    val rank: Int = 0,
    val isMe: Boolean = false
)

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    fun fetchUserById(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            val result = userRepository.getUserWithRankAndReview(userId)

            result.fold(
                onSuccess = { (user, hasReviewed, rank) ->
                    _uiState.value = _uiState.value.copy(
                        user = user,
                        isLoading = false,
                        hasReviewed = hasReviewed,
                        rank = rank ?: 0,
                        isMe = user.uid == authRepository.getCurrentUser()?.uid
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
}