package com.example.gostreetball.ui.auth

import com.example.gostreetball.data.model.auth.RegisterData

data class RegisterUiState(
    val registerData: RegisterData = RegisterData(),
    val currentStep: Int = 1,
    val errorMessage: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false
)