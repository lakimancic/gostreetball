package com.example.gostreetball.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gostreetball.data.model.Court
import com.example.gostreetball.data.model.GameInvite
import com.example.gostreetball.data.model.InviteStatus
import com.example.gostreetball.data.model.User
import com.example.gostreetball.data.repo.AuthRepository
import com.example.gostreetball.data.repo.CourtRepository
import com.example.gostreetball.data.repo.GameRepository
import com.example.gostreetball.data.repo.UserRepository
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState (
    val court: Court? = null,
    val invite: GameInvite? = null,
    val inviterUser: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val courtRepository: CourtRepository,
    private val userRepository: UserRepository,
    private val gameRepository: GameRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var latestInviteListener: ListenerRegistration? = null

    fun fetchCurrentCourt() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val user = userRepository.getCurrentUser().getOrThrow()
                val courtId = user.currentCourt
                if (!courtId.isNullOrEmpty()) {
                    val courtResult = courtRepository.getCourt(courtId)
                    courtResult.onSuccess { (court, _) ->
                        _uiState.value = _uiState.value.copy(court = court, isLoading = false)
                    }.onFailure { e ->
                        _uiState.value = _uiState.value.copy(court = null, isLoading = false, error = e.message)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(court = null, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun observeLatestInvite() {
        latestInviteListener?.remove()
        latestInviteListener = gameRepository.observeLatestInvite(
            onChange = { invite ->
                _uiState.update { it.copy(invite = invite, error = null) }
            },
            onError = { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        )
    }

    fun respondToInvite(inviteId: String, status: InviteStatus) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = gameRepository.updateInviteStatus(inviteId, status)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        latestInviteListener?.remove()
    }

    fun leaveCourt() {
        viewModelScope.launch {
            userRepository.leaveCourt()

            _uiState.value = _uiState.value.copy(
                invite = null,
                inviterUser = null,
                court = null,
                isLoading = false,
                error = null
            )
        }
    }
}