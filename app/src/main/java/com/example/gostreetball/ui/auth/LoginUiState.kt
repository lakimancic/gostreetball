package com.example.gostreetball.ui.auth

import com.example.gostreetball.data.model.auth.LoginData

data class LoginUiState(
    val loginData: LoginData = LoginData(),
    val passwordVisible: Boolean = false,
    val errorMessage: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false
)