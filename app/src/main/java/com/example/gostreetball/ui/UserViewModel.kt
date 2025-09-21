package com.example.gostreetball.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gostreetball.data.model.Game
import com.example.gostreetball.data.model.User
import com.example.gostreetball.data.repo.AuthRepository
import com.example.gostreetball.data.repo.GameRepository
import com.example.gostreetball.data.repo.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class UserUiState (
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasReviewed: Boolean = false,
    val rank: Int = 0,
    val isMe: Boolean = false,
    val games: List<Pair<Game, Boolean>> = emptyList()
)

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository
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

    fun fetchMyGames(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val games = gameRepository.getUserGames(userId)

                _uiState.update { currentState ->
                    currentState.copy(
                        games = games
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