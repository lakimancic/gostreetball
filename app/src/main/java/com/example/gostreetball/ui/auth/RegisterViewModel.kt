package com.example.gostreetball.ui.auth

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gostreetball.data.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun updateCredentials(username: String, password: String, confirmPassword: String) = _uiState.update {
        it.copy(
            registerData = it.registerData.copy(
                username = username,
                password = password,
                confirmPassword = confirmPassword
            ),
            currentStep = 1
        )
    }

    private fun checkCredentials(): Boolean {
        val username = uiState.value.registerData.username
        val password = uiState.value.registerData.password
        val confirmPassword = uiState.value.registerData.confirmPassword

        if (password != confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords don't match") }
            return false
        }

        if (username.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Username is required") }
            return false
        }

        val usernameRegex = "^[a-zA-Z0-9_]+$".toRegex()
        if (!usernameRegex.matches(username)) {
            _uiState.update { it.copy(errorMessage = "Username can only contain letters, numbers, and _") }
            return false
        }

        if (password.length < 8) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 8 characters") }
            return false
        }

        var hasUpper = false
        var hasLower = false
        var hasDigit = false
        var hasSpecial = false

        password.forEach { char ->
            when {
                char.isUpperCase() -> hasUpper = true
                char.isLowerCase() -> hasLower = true
                char.isDigit() -> hasDigit = true
                !char.isLetterOrDigit() -> hasSpecial = true
            }
        }

        if (!hasUpper) {
            _uiState.update { it.copy(errorMessage = "Password must contain at least one uppercase letter") }
            return false
        }

        if (!hasLower) {
            _uiState.update { it.copy(errorMessage = "Password must contain at least one lowercase letter") }
            return false
        }

        if (!hasDigit) {
            _uiState.update { it.copy(errorMessage = "Password must contain at least one digit") }
            return false
        }

        if (!hasSpecial) {
            _uiState.update { it.copy(errorMessage = "Password must contain at least one special character") }
            return false
        }

        _uiState.update { it.copy(errorMessage = "") }
        return true
    }

    fun updatePersonalInfo(firstName: String, lastName: String, phone: String) = _uiState.update {
        it.copy(
            registerData = it.registerData.copy(
                firstName = firstName,
                lastName = lastName,
                phone = phone
            ),
            currentStep = 2
        )
    }

    private fun checkPersonalInfo(): Boolean {
        val firstName = uiState.value.registerData.firstName
        val lastName = uiState.value.registerData.lastName
        val phone = uiState.value.registerData.phone

        if (firstName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "First name is required") }
            return false
        }

        if (!firstName.matches("^[A-Z][a-zA-Z]*$".toRegex())) {
            _uiState.update { it.copy(errorMessage = "Invalid First name") }
            return false
        }

        if (lastName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Last name is required") }
            return false
        }

        if (!lastName.matches("^[A-Z][a-zA-Z]*$".toRegex())) {
            _uiState.update { it.copy(errorMessage = "Invalid Last name") }
            return false
        }

        if (phone.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Phone number is required") }
            return false
        }

        if (!phone.matches("^[0-9+\\-() ]+$".toRegex())) {
            _uiState.update { it.copy(errorMessage = "Invalid Phone number") }
            return false
        }

        _uiState.update { it.copy(errorMessage = "") }
        return true
    }

    fun updateProfileImage(bitmap: Bitmap?) = _uiState.update {
        it.copy(
            registerData = it.registerData.copy(
                profileImage = bitmap?.asImageBitmap()
            ),
            currentStep = 3
        )
    }

    fun createAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = "") }

            val result = authRepository.register(uiState.value.registerData)

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
                        currentStep = 1,
                        errorMessage = e.message?.replace("email address", "username") ?: "Registration failed"
                    )
                }
            }
        }
    }

    fun nextStep() {
        if (_uiState.value.currentStep == 1 && !checkCredentials())
            return
        if (_uiState.value.currentStep == 2 && !checkPersonalInfo())
            return

        _uiState.update {
            it.copy(currentStep = it.currentStep + 1)
        }
    }

    fun previousStep() = _uiState.update {
        it.copy(currentStep = it.currentStep - 1)
    }
}