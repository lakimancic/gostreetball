package com.example.gostreetball.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gostreetball.data.model.Game
import com.example.gostreetball.data.repo.GameRepository
import com.example.gostreetball.ui.AddUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OneVsOneUiState (
    val game: Game? = null,
    val scoreA: Int = 0,
    val scoreB: Int = 0,
    val playerWithBall: Int = 0,
    val error: String? = null,
    val isLoading: Boolean = false,
)

@HiltViewModel
class OneVsOneViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(OneVsOneUiState())
    val uiState: StateFlow<OneVsOneUiState> = _uiState.asStateFlow()

    fun twoPointer() {
        _uiState.update { current ->
            val updatedState = when (current.playerWithBall) {
                0 -> current.copy(scoreA = current.scoreA + 2)
                1 -> current.copy(scoreB = current.scoreB + 2)
                else -> current
            }

            checkGameFinished(updatedState)
        }
    }

    fun threePointer() {
        _uiState.update { current ->
            val updatedState = when (current.playerWithBall) {
                0 -> current.copy(scoreA = current.scoreA + 3)
                1 -> current.copy(scoreB = current.scoreB + 3)
                else -> current
            }

            checkGameFinished(updatedState)
        }
    }

    private fun checkGameFinished(state: OneVsOneUiState): OneVsOneUiState {
        val game = state.game ?: return state

        val target = game.settings.targetPoints
        val winByTwo = game.settings.winByTwo

        val scoreA = state.scoreA
        val scoreB = state.scoreB

        val winner = when {
            scoreA >= target && (!winByTwo || scoreA - scoreB >= 2) -> 0
            scoreB >= target && (!winByTwo || scoreB - scoreA >= 2) -> 1
            else -> -1
        }

        return if (winner != -1) {
            finishGame(winner)
            state
        } else {
            state
        }
    }

    private fun finishGame(winner: Int) {
        // TODO: handle finishing the game
    }

    fun switchPossession() {
        _uiState.update { current ->
            current.copy(playerWithBall = if (current.playerWithBall == 0) 1 else 0)
        }
    }

    fun resetScores() {
        _uiState.update { current ->
            current.copy(scoreA = 0, scoreB = 0)
        }
    }

    fun loadGame(gameId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = gameRepository.getGame(gameId)

            result.onSuccess { game ->
                _uiState.update {
                    it.copy(
                        game = game,
                        scoreA = 0,
                        scoreB = 0,
                        playerWithBall = 0,
                        isLoading = false,
                        error = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Unknown error"
                    )
                }
            }
        }
    }
}