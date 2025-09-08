package com.example.gostreetball.data.model.auth

import androidx.compose.ui.graphics.ImageBitmap

data class RegisterData(
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val profileImage: ImageBitmap? = null
)