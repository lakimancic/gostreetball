package com.example.gostreetball.data.model

data class Review(
    val stars: Int = 0,
    val text: String = "",
    val isForCourt: Boolean = true,
    val userId: String = "",
    val itemId: String = ""
)