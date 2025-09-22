package com.example.gostreetball.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gostreetball.data.model.Game
import com.example.gostreetball.data.model.User
import com.example.gostreetball.data.repo.GameRepository
import com.example.gostreetball.data.repo.UserRepository
import com.example.gostreetball.ui.AddUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

data class OneVsOneUiState (
    val game: Game? = null,
    val scoreA: Int = 0,
    val scoreB: Int = 0,
    val playerWithBall: Int = 0,
    val error: String? = null,
    var players: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val winner: User? = null
)

@HiltViewModel
class OneVsOneViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(OneVsOneUiState())
    val uiState: StateFlow<OneVsOneUiState> = _uiState.asStateFlow()

    fun twoPointer() {
        _uiState.update { current ->
            val updatedState = when (current.playerWithBall) {
                0 -> current.copy(scoreA = current.scoreA + 1)
                1 -> current.copy(scoreB = current.scoreB + 1)
                else -> current
            }

            val afterFinishCheck = checkGameFinished(updatedState)

            if (afterFinishCheck == updatedState) {
                if (current.game?.settings?.makeItTakeIt != true) {
                    afterFinishCheck.copy(playerWithBall = if (current.playerWithBall == 0) 1 else 0)
                } else {
                    afterFinishCheck
                }
            } else {
                afterFinishCheck
            }
        }
    }

    fun threePointer() {
        _uiState.update { current ->
            val updatedState = when (current.playerWithBall) {
                0 -> current.copy(scoreA = current.scoreA + 2)
                1 -> current.copy(scoreB = current.scoreB + 2)
                else -> current
            }

            val afterFinishCheck = checkGameFinished(updatedState)

            if (afterFinishCheck == updatedState) {
                if (current.game?.settings?.makeItTakeIt != true) {
                    afterFinishCheck.copy(playerWithBall = if (current.playerWithBall == 0) 1 else 0)
                } else {
                    afterFinishCheck
                }
            } else {
                afterFinishCheck
            }
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
            state.copy(winner = state.players.getOrNull(winner))
        } else {
            state
        }
    }

    private fun finishGame(winner: Int) {
        val currentState = _uiState.value
        val game = currentState.game ?: return

        viewModelScope.launch {
            gameRepository.updateScore(
                game = game,
                playerOrders = currentState.players,
                winnerIndex = winner
            )
        }
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

    fun loadGameWithPlayers(gameId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val gameResult = gameRepository.getGame(gameId)

            gameResult.onSuccess { game ->
                if (game != null) {
                    val playersResult = userRepository.getUsersForIds(game.players)

                    playersResult.onSuccess { users ->
                        _uiState.update {
                            it.copy(
                                game = game,
                                scoreA = 0,
                                scoreB = 0,
                                playerWithBall = Random.nextBits(1),
                                players = users,
                                isLoading = false,
                                error = null
                            )
                        }
                    }.onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                game = game,
                                scoreA = 0,
                                scoreB = 0,
                                playerWithBall = 0,
                                isLoading = false,
                                error = throwable.message ?: "Failed to load players"
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            game = null,
                            isLoading = false,
                            error = "Game not found"
                        )
                    }
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