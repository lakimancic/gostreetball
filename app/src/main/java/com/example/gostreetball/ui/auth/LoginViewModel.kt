package com.example.gostreetball.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gostreetball.data.repo.AuthRepository
import com.example.gostreetball.data.repo.UserRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val firebaseMessaging: FirebaseMessaging
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onUsernameChange(v: String) = _uiState.update {
        it.copy(loginData = it.loginData.copy(username = v))
    }

    fun onPasswordChange(v: String) = _uiState.update {
        it.copy(loginData = it.loginData.copy(password = v))
    }

    fun togglePasswordVisibility() = _uiState.update {
        it.copy(passwordVisible = !it.passwordVisible)
    }

    fun login() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = "") }

            val result = authRepository.login(uiState.value.loginData)

            result.onSuccess { _ ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        errorMessage = ""
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message?.replace("email address", "username") ?: "Login failed"
                    )
                }
            }
        }
    }
}