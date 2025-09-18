package com.example.gostreetball.ui

import androidx.lifecycle.ViewModel
import com.example.gostreetball.data.model.GameSettings
import com.example.gostreetball.data.model.User
import com.example.gostreetball.data.repo.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class GameSetupUiState (
    val gameSetup: GameSettings = GameSettings(),
    val playersA: List<Pair<String, Boolean>> = emptyList(),
    val playersB: List<Pair<String, Boolean>> = emptyList(),
    val availablePlayers: List<User> = emptyList(),
    val gameId: String = "",
)

@HiltViewModel
class GameSetupViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(GameSetupUiState())
    val uiState: StateFlow<GameSetupUiState> = _uiState.asStateFlow()

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


}