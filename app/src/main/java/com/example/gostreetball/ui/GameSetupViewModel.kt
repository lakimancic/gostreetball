package com.example.gostreetball.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gostreetball.data.model.Game
import com.example.gostreetball.data.model.GameSettings
import com.example.gostreetball.data.model.GameType
import com.example.gostreetball.data.model.InviteStatus
import com.example.gostreetball.data.model.User
import com.example.gostreetball.data.repo.AuthRepository
import com.example.gostreetball.data.repo.GameRepository
import com.example.gostreetball.data.repo.UserRepository
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class GameSetupUiState (
    val gameSetup: GameSettings = GameSettings(),
    val playersA: List<Pair<String, Boolean>> = emptyList(),
    val playersB: List<Pair<String, Boolean>> = emptyList(),
    val activePlayers: List<User> = emptyList(),
    val gameType: GameType = GameType.ONE_VS_ONE,
    val gameId: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class PlayerInviteStatus(
    val user: User,
    val statusA: InviteStatus = InviteStatus.REJECTED,
    val statusB: InviteStatus = InviteStatus.REJECTED
)

@HiltViewModel
class GameSetupViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(GameSetupUiState())
    val uiState: StateFlow<GameSetupUiState> = _uiState.asStateFlow()

    private var activeUsersListener: ListenerRegistration? = null
    private var invitesListener: ListenerRegistration? = null

    private val _navigateToGame = MutableSharedFlow<Unit>()
    val navigateToGame: SharedFlow<Unit> = _navigateToGame

    val canStartGame: StateFlow<Boolean> = _uiState
        .map { state ->
            val type = state.gameType
            val aSize = state.playersA.count { it.second }
            val bSize = state.playersB.count { it.second }

            when (type) {
                GameType.ONE_VS_ONE -> aSize == 1 && bSize == 1
                GameType.THREE_X_THREE -> aSize == 3 && bSize == 3
                GameType.SEVEN_UP, GameType.AROUND_THE_WORLD -> aSize in 2..6
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val canInviteA: StateFlow<Boolean> = uiState
        .map { state ->
            val type = state.gameType
            val aCount = state.playersA.size
            when (type) {
                GameType.ONE_VS_ONE -> aCount < 1
                GameType.THREE_X_THREE -> aCount < 3
                GameType.SEVEN_UP, GameType.AROUND_THE_WORLD -> aCount < 6
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val canInviteB: StateFlow<Boolean> = uiState
        .map { state ->
            val type = state.gameType
            val bCount = state.playersB.size
            when (type) {
                GameType.ONE_VS_ONE -> bCount < 1
                GameType.THREE_X_THREE -> bCount < 3
                GameType.SEVEN_UP, GameType.AROUND_THE_WORLD -> false
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val playerInviteStatuses: StateFlow<List<PlayerInviteStatus>> =
        uiState.map { state ->
            val playersA = state.playersA.toMap()
            val playersB = state.playersB.toMap()

            state.activePlayers.map { user ->
                PlayerInviteStatus(
                    user = user,
                    statusA = when (playersA[user.uid]) {
                        true -> InviteStatus.ACCEPTED
                        false -> InviteStatus.PENDING
                        null -> InviteStatus.REJECTED
                    },
                    statusB = when (playersB[user.uid]) {
                        true -> InviteStatus.ACCEPTED
                        false -> InviteStatus.PENDING
                        null -> InviteStatus.REJECTED
                    }
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setTargetPoints(points: Int) {
        _uiState.update { current ->
            current.copy(
                gameSetup = current.gameSetup.copy(
                    targetPoints = points
                )
            )
        }
    }

    fun toggleWinByTwo() {
        _uiState.update { current ->
            current.copy(
                gameSetup = current.gameSetup.copy(
                    winByTwo = !current.gameSetup.winByTwo
                )
            )
        }
    }

    fun toggleMakeItTakeIt() {
        _uiState.update { current ->
            current.copy(
                gameSetup = current.gameSetup.copy(
                    makeItTakeIt = !current.gameSetup.makeItTakeIt
                )
            )
        }
    }

    fun toggleMissCount() {
        _uiState.update { current ->
            current.copy(
                gameSetup = current.gameSetup.copy(
                    missCount = !current.gameSetup.missCount
                )
            )
        }
    }

    fun toggleLongRoute() {
        _uiState.update { current ->
            current.copy(
                gameSetup = current.gameSetup.copy(
                    longRoute = !current.gameSetup.longRoute
                )
            )
        }
    }

    fun setGameType(type: GameType) {
        _uiState.update { state ->
            state.copy(
                gameType = type,
                playersA = emptyList(),
                playersB = emptyList()
            )
        }
    }

    fun observeActivePlayers(courtId: String) {
        activeUsersListener?.remove()
        activeUsersListener = userRepository.observeActiveUsersInCourt(
            courtId = courtId,
            onChange = { activeUsers ->
                _uiState.update { current ->
                    current.copy(
                        activePlayers = activeUsers,
                        error = null
                    ).cleanColumns(activeUsers)
                }
            },
            onError = { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        )
    }

    fun observeInvites(gameId: String) {
        invitesListener?.remove()
        invitesListener = gameRepository.observeFinishedInvitesForGame(
            gameId = gameId,
            onChange = { invites ->
                val acceptedIds = invites.filter { it.status == InviteStatus.ACCEPTED }.map { it.toUserId }
                val rejectedIds = invites.filter { it.status == InviteStatus.REJECTED }.map { it.toUserId }

                _uiState.update { current ->
                    current.copy(
                        playersA = current.playersA.updateAcceptanceAndClean(acceptedIds, rejectedIds),
                        playersB = current.playersB.updateAcceptanceAndClean(acceptedIds, rejectedIds),
                        error = null
                    )
                }
            }
        )
    }

    fun cancelInvite(toUserId: String) {
        val gameId = _uiState.value.gameId
        if (gameId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = gameRepository.cancelInvite(toUserId, gameId)

            result.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun createGame() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val newGame = Game(
                type = uiState.value.gameType,
                judgeId = authRepository.getCurrentUser()?.uid ?: "",
                settings = uiState.value.gameSetup
            )

            val result = gameRepository.createGame(newGame)
            result.onSuccess { gameId ->
                _uiState.update { it.copy(gameId = gameId, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun startGame() {
        val gameId = _uiState.value.gameId
        if (gameId.isBlank()) return

        val players = (_uiState.value.playersA.map { it.first } + _uiState.value.playersB.map { it.first })

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = gameRepository.startGame(gameId, players)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                _navigateToGame.emit(Unit);
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun sendInvite(toUser: User, isTeamA: Boolean) {
        val gameId = _uiState.value.gameId
        val gameType = _uiState.value.gameType

        if (gameId.isBlank()) {
            _uiState.update { it.copy(error = "Game ID is blank") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = gameRepository.sendInvite(toUser.uid, gameId, gameType)
            result.onSuccess { inviteId ->
                _uiState.update { current ->
                    if (isTeamA) {
                        current.copy(
                            playersA = current.playersA + (toUser.uid to false),
                            isLoading = false
                        )
                    } else {
                        current.copy(
                            playersB = current.playersB + (toUser.uid to false),
                            isLoading = false
                        )
                    }
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeUsersListener?.remove()
        invitesListener?.remove()
    }
}

private fun GameSetupUiState.cleanColumns(activeUsers: List<User>): GameSetupUiState {
    val activeIds = activeUsers.map { it.uid }.toSet()
    return copy(
        playersA = playersA.filter { activeIds.contains(it.first) },
        playersB = playersB.filter { activeIds.contains(it.first) }
    )
}

private fun List<Pair<String, Boolean>>.updateAcceptanceAndClean(
    acceptedIds: List<String>,
    rejectedIds: List<String>
): List<Pair<String, Boolean>> {
    return this
        .filterNot { (id, _) -> rejectedIds.contains(id) }
        .map { (id, _) -> id to acceptedIds.contains(id) }
}
