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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AroundTheWorldUiState (
    val game: Game? = null,
    val positions: List<Int> = emptyList(),
    val playerWithBall: Int = 0,
    val players: List<User> = emptyList(),
    val error: String? = null,
    val isLoading: Boolean = false,
    val isFinished: Boolean = false
)

@HiltViewModel
class AroundTheWorldViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AroundTheWorldUiState())
    val uiState: StateFlow<AroundTheWorldUiState> = _uiState.asStateFlow()

    val playersOnCourt: StateFlow<List<Pair<User, CourtPosition>>> = uiState.map { state ->
        val route = getRoute()
        val positions = state.positions
        val players = state.players

        players.mapIndexedNotNull { index, user ->
            val posIndex = positions.getOrNull(index) ?: 0
            if (posIndex >= route.size) return@mapIndexedNotNull null
            user to route[posIndex]
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    private val longRoute = buildList {
        add(CourtPosition.BelowBasket)
        add(CourtPosition.KeyArea(true, 0f))
        add(CourtPosition.KeyArea(true, 0.5f))
        add(CourtPosition.KeyArea(true, 1f))
        add(CourtPosition.FreeThrow)
        add(CourtPosition.KeyArea(false, 1f))
        add(CourtPosition.KeyArea(false, 0.5f))
        add(CourtPosition.KeyArea(false, 0f))
        add(CourtPosition.ThreePointer(0f))
        add(CourtPosition.ThreePointer(0.25f))
        add(CourtPosition.ThreePointer(0.5f))
        add(CourtPosition.ThreePointer(0.75f))
        add(CourtPosition.ThreePointer(1f))
        add(CourtPosition.BelowBasket)
    }

    private val shortRoute = buildList {
        add(CourtPosition.BelowBasket)
        add(CourtPosition.KeyArea(true, 0.5f))
        add(CourtPosition.FreeThrow)
        add(CourtPosition.KeyArea(false, 0.5f))
        add(CourtPosition.ThreePointer(0f))
        add(CourtPosition.ThreePointer(0.5f))
        add(CourtPosition.ThreePointer(1f))
        add(CourtPosition.BelowBasket)
    }

    private fun getRoute(): List<CourtPosition> {
        val game = _uiState.value.game
        return if (game?.settings?.longRoute == true) longRoute else shortRoute
    }

    fun hitShot() {
        _uiState.update { current ->
            val route = getRoute()
            val positions = current.positions.toMutableList()

            val player = current.playerWithBall
            val currentIndex = positions.getOrElse(player) { 0 }
            val nextIndex = currentIndex + 1

            positions[player] = nextIndex
            if (nextIndex >= route.size) {
                finishGame(player, positions)
                current.copy(isFinished = true)
            } else {
                current.copy(positions = positions)
            }
        }
    }

    fun missShot() {
        _uiState.update { current ->
            val game = current.game ?: return@update current
            val playerCount = game.players.size
            if (playerCount == 0) return@update current

            val nextPlayer = (current.playerWithBall + 1) % playerCount

            current.copy(playerWithBall = nextPlayer)
        }
    }

    private fun finishGame(winner: Int, positions: List<Int>) {
        val currentState = _uiState.value
        val game = currentState.game ?: return
        val players = currentState.players

        if (players.isEmpty() || positions.size != players.size) return

        val sortedPlayers = players
            .zip(positions)
            .sortedByDescending { it.second }
            .map { it.first }

        val winnerIndex = players.indexOf(sortedPlayers.first())

        viewModelScope.launch {
            gameRepository.updateScore(
                game = game,
                playerOrders = sortedPlayers,
                winnerIndex = winnerIndex
            )
        }
    }

    fun loadGameWithPlayers(gameId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val gameResult = gameRepository.getGame(gameId)

            gameResult.onSuccess { game ->
                if (game != null) {
                    val shuffledGame = game.copy(players = game.players.shuffled())

                    val playersResult = userRepository.getUsersForIds(shuffledGame.players)

                    playersResult.onSuccess { users ->
                        _uiState.update {
                            it.copy(
                                game = shuffledGame,
                                positions = shuffledGame.players.map { 0 },
                                playerWithBall = 0,
                                players = users,
                                isLoading = false,
                                error = null
                            )
                        }
                    }.onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                game = shuffledGame,
                                positions = shuffledGame.players.map { 0 },
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
                            positions = emptyList(),
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