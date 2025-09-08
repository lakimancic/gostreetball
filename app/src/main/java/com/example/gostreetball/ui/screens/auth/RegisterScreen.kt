package com.example.gostreetball.ui.screens.auth

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gostreetball.ui.auth.RegisterViewModel
import com.example.gostreetball.ui.screens.auth.register_parts.CredentialsStep
import com.example.gostreetball.ui.screens.auth.register_parts.PersonalInfoStep
import com.example.gostreetball.ui.screens.auth.register_parts.ProfileImageStep
import com.example.gostreetball.ui.theme.GoStreetBallTheme
import com.example.gostreetball.utils.rememberCameraCapture
import com.example.gostreetball.utils.rememberImagePicker

@Composable
fun RegistrationScreen(
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = hiltViewModel(),
    navigateBack: () -> Unit,
    navigateToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val galleryLauncher = rememberImagePicker { bitmap ->
        viewModel.updateProfileImage(bitmap)
    }

    val cameraLauncher = rememberCameraCapture { bitmap ->
        viewModel.updateProfileImage(bitmap)
    }

    if (uiState.isSuccess) {
        LaunchedEffect(Unit) {
            navigateToLogin()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return
        }
        LinearProgressIndicator(
            progress = { uiState.currentStep / 3f },
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = 15.dp)
        )
        Spacer(Modifier.height(16.dp))
        when (uiState.currentStep) {
            1 -> CredentialsStep(
                username = uiState.registerData.username,
                password = uiState.registerData.password,
                error = uiState.errorMessage,
                confirmPassword = uiState.registerData.confirmPassword,
                onUsernameChange = { viewModel.updateCredentials(it, uiState.registerData.password, uiState.registerData.confirmPassword) },
                onPasswordChange = { viewModel.updateCredentials(uiState.registerData.username, it, uiState.registerData.confirmPassword) },
                onConfirmPasswordChange = { viewModel.updateCredentials(uiState.registerData.username, uiState.registerData.password, it) },
                navigateNext = { viewModel.nextStep() },
                navigateBack = navigateBack,
                navigateToLogin = navigateToLogin,
            )
            2 -> PersonalInfoStep(
                firstName = uiState.registerData.firstName,
                lastName = uiState.registerData.lastName,
                phone = uiState.registerData.phone,
                error = uiState.errorMessage,
                onFirstNameChange = { viewModel.updatePersonalInfo(it, uiState.registerData.lastName, uiState.registerData.phone) },
                onLastNameChange = { viewModel.updatePersonalInfo(uiState.registerData.firstName, it, uiState.registerData.phone) },
                onPhoneChange = { viewModel.updatePersonalInfo(uiState.registerData.firstName, uiState.registerData.lastName, it) },
                navigateNext = { viewModel.nextStep() },
                navigateBack = { viewModel.previousStep() },
                navigateToLogin = navigateToLogin,
            )
            3 -> ProfileImageStep(
                image = uiState.registerData.profileImage,
                error = uiState.errorMessage,
                registerAccount = { viewModel.createAccount() },
                navigateBack = { viewModel.previousStep() },
                navigateToLogin = navigateToLogin,
                launchCamera = { cameraLauncher.launch(null) },
                launchGallery = { galleryLauncher.launch("image/*") }
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
    name = "Dark Preview"
)
@Composable
private fun RegisterScreenPreview() {
    GoStreetBallTheme {
        RegistrationScreen(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background),
            navigateBack = {},
            navigateToLogin = {}
        )
    }
}