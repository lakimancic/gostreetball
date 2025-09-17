package com.example.gostreetball.ui

import android.app.Application
import android.content.res.Resources.Theme
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gostreetball.data.local.AppPreferences
import com.example.gostreetball.data.local.ThemeEnum
import com.example.gostreetball.data.model.BoardType
import com.example.gostreetball.data.model.Court
import com.example.gostreetball.data.model.CourtType
import com.example.gostreetball.data.repo.AuthRepository
import com.example.gostreetball.data.repo.UserRepository
import com.example.gostreetball.utils.loadImageBitmapFromUrl
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ProfileUiState (
    val isLoading: Boolean = false,
    val error: String? = null,
    val image: ImageBitmap? = null,
    val oldPassword: String = "",
    val newPassword: String = ""
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val application: Application,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val preferences: AppPreferences
) : ViewModel() {
    private val _isTrackingOn = MutableStateFlow(false)
    val isTrackingOn: StateFlow<Boolean> = _isTrackingOn.asStateFlow()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _checkRadius = MutableStateFlow(100)
    val checkRadius: StateFlow<Int> = _checkRadius.asStateFlow()

    private val _theme = MutableStateFlow(ThemeEnum.LIGHT)
    val theme: StateFlow<ThemeEnum> = _theme

    init {
        viewModelScope.launch {
            preferences.isTrackingEnabled.collectLatest { enabled ->
                _isTrackingOn.value = enabled
            }
        }

        viewModelScope.launch {
            preferences.checkRadiusMeters.collectLatest { radius ->
                _checkRadius.value = radius
            }
        }

        viewModelScope.launch {
            preferences.selectedTheme.collectLatest { theme ->
                _theme.value = theme
            }
        }
    }

    fun getCurrentUserId(): String {
        return authRepository.getCurrentUser()?.uid ?: ""
    }

    fun setCheckRadius(radius: Int) {
        viewModelScope.launch {
            preferences.setCheckRadiusMeters(radius)
        }
    }

    fun toggleTracking() {
        viewModelScope.launch {
            preferences.setTrackingEnabled(!isTrackingOn.value)
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            preferences.saveTheme(if (theme.value == ThemeEnum.LIGHT) ThemeEnum.DARK else ThemeEnum.LIGHT )
        }
    }

    fun loadOwnProfileImage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val firebaseUser = authRepository.getCurrentUser() ?: throw Exception("No logged in user")

                val result = userRepository.getProfilePictureUrl(firebaseUser.uid)

                result.fold(
                    onSuccess = { imageUrl ->
                        val context = application
                        val bitmap = if (imageUrl.isNotBlank()) {
                            loadImageBitmapFromUrl(context, imageUrl)
                        } else null

                        _uiState.value = _uiState.value.copy(
                            image = bitmap,
                            isLoading = false
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            image = null,
                            isLoading = false,
                            error = error.message
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    image = null,
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun changeProfileImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = userRepository.updateProfilePicture(bitmap)
            result.fold(
                onSuccess = { _ ->
                    _uiState.value = _uiState.value.copy(
                        image = bitmap.asImageBitmap(),
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun updateOldPassword(value: String) {
        _uiState.value = _uiState.value.copy(oldPassword = value)
    }

    fun updateNewPassword(value: String) {
        _uiState.value = _uiState.value.copy(newPassword = value)
    }

    fun changePassword() {
        val oldPass = _uiState.value.oldPassword
        val newPass = _uiState.value.newPassword
        val currentUser = authRepository.getCurrentUser() ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val credential = EmailAuthProvider.getCredential(
                    currentUser.email ?: "",
                    oldPass
                )
                currentUser.reauthenticate(credential).await()

                currentUser.updatePassword(newPass).await()

                _uiState.value = _uiState.value.copy(
                    oldPassword = "",
                    newPassword = "",
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }
}