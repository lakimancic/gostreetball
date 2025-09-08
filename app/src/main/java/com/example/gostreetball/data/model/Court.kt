package com.example.gostreetball.data.model

import com.google.firebase.firestore.GeoPoint

enum class CourtType {
    ASPHALT,
    RUBBER,
    SYNTHETIC
}

data class Court (
    val id: String = "",
    val name: String = "",
    val type: CourtType = CourtType.ASPHALT,
    val location: GeoPoint? = null,
    val rating: Double = 0.0,
    val reviews: List<Review> = emptyList(),
    val games: List<Game> = emptyList(),
    val imageUrl: String = ""
)