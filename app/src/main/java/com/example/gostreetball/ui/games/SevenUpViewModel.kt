package com.example.gostreetball.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gostreetball.data.model.Game
import com.example.gostreetball.data.model.User
import com.example.gostreetball.data.repo.GameRepository
import com.example.gostreetball.data.repo.UserRepository
import com.example.gostreetball.ui.AddUiState
import com.example.gostreetball.ui.screens.games.CourtPosition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SevenUpUiState (
    val game: Game? = null,
    val playerWithBall: Int = 0,
    val accumulated: Int = 0,
    val scores: List<Int> = emptyList(),
    val playerOrders: List<User> = emptyList(),
    val players: List<User> = emptyList(),
    val error: String? = null,
    val isLoading: Boolean = false,
    val winner: User? = null
)

@HiltViewModel
class SevenUpViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SevenUpUiState())
    val uiState: StateFlow<SevenUpUiState> = _uiState.asStateFlow()

    val playersOnCourt: StateFlow<List<Pair<User, CourtPosition>>> = uiState.map { state ->
        val alivePlayers = state.players.filterIndexed { index, _ ->
            index !in state.playerOrders.map { state.players.indexOf(it) }
        }

        val offsets = when (alivePlayers.size) {
            2 -> listOf(0.25f, 0.75f)
            3 -> listOf(0.25f, 0.5f, 0.75f)
            4 -> listOf(0.2f, 0.4f, 0.6f, 0.8f)
            5 -> listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
            6 -> listOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f)
            else -> emptyList()
        }

        alivePlayers.mapIndexed { i, user ->
            user to CourtPosition.ThreePointer(offsets.getOrNull(i) ?: 0f)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    fun hitShot() {
        _uiState.update { current ->
            val game = current.game ?: return@update current
            val playerCount = game.players.size
            if (playerCount == 0) return@update current

            val nextPlayer = findNextPlayer(current, current.players)

            current.copy(
                accumulated = current.accumulated + 1,
                playerWithBall = nextPlayer
            )
        }
    }

    fun missShot() {
        _uiState.update { current ->
            val game = current.game ?: return@update current
            val players = current.players
            val playerCount = game.players.size
            if (playerCount == 0) return@update current

            val scores = current.scores.toMutableList()
            if (scores.size < playerCount) {
                scores.addAll(List(playerCount - scores.size) { 0 })
            }

            scores[current.playerWithBall] += current.accumulated

            val playerOrders = current.playerOrders.toMutableList()
            val currentPlayer = players.getOrNull(current.playerWithBall) ?: return@update current

            if (scores[current.playerWithBall] >= 7 && !playerOrders.contains(currentPlayer)) {
                playerOrders.add(currentPlayer)
            }

            if (playerOrders.size == playerCount - 1) {
                val winnerIndex = players.indexOfFirst { !playerOrders.contains(it) }
                val winner = players[winnerIndex]

                playerOrders.add(winner)
                finishGame(winnerIndex, playerOrders)

                return@update current.copy(
                    scores = scores,
                    accumulated = 0,
                    playerOrders = playerOrders,
                    playerWithBall = winnerIndex,
                    winner = current.players.getOrNull(winnerIndex)
                )
            }

            val nextPlayer = findNextPlayer(current.copy(scores = scores, playerOrders = playerOrders), players)

            current.copy(
                scores = scores,
                accumulated = 0,
                playerOrders = playerOrders,
                playerWithBall = nextPlayer
            )
        }
    }

    private fun findNextPlayer(state: SevenUpUiState, players: List<User>): Int {
        val playerCount = players.size
        var next = (state.playerWithBall + 1) % playerCount

        while (state.playerOrders.contains(players[next])) {
            next = (next + 1) % playerCount
            if (state.playerOrders.size == playerCount - 1) break
        }

        return next
    }


    private fun finishGame(winner: Int, orders: List<User>) {
        val currentState = _uiState.value
        val game = currentState.game ?: return

        viewModelScope.launch {
            gameRepository.updateScore(
                game = game,
                playerOrders = orders.reversed(),
                winnerIndex = winner
            )
        }
    }

    fun loadGameWithPlayers(gameId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val gameResult = gameRepository.getGame(gameId)

            gameResult.onSuccess { game ->
                val shuffledGame = game?.copy(players = game.players.shuffled())

                if (shuffledGame != null) {
                    val playersResult = userRepository.getUsersForIds(shuffledGame.players)

                    playersResult.onSuccess { users ->
                        _uiState.update {
                            it.copy(
                                game = shuffledGame,
                                playerWithBall = 0,
                                scores = shuffledGame.players.map { 0 },
                                players = users,
                                isLoading = false,
                                error = null
                            )
                        }
                    }.onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                game = shuffledGame,
                                playerWithBall = 0,
                                scores = shuffledGame.players.map { 0 },
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