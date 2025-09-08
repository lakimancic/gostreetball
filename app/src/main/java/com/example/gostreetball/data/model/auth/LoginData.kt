package com.example.gostreetball.data.model.auth

data class LoginData(
    val username: String = "",
    val password: String = ""
) {
    fun isValid() = username.isNotBlank() && password.isNotBlank()
}