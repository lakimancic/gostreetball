package com.example.gostreetball.data.model

import com.google.firebase.firestore.GeoPoint

enum class CourtType {
    ASPHALT,
    RUBBER,
    SYNTHETIC
}

enum class BoardType {
    GLASS,
    WOOD,
    PLASTIC
}

data class Court (
    val id: String = "",
    val name: String = "",
    val type: CourtType = CourtType.ASPHALT,
    val boardType: BoardType = BoardType.WOOD,
    val location: GeoPoint? = null,
    val geoHash: String? = null,
    val rating: Double = 0.0,
    val city: String = "",
    val country: String = "",
    val reviewCount: Int = 0,
    val gameCount: Int = 0,
    val imageUrl: String = "",
    val coefficient: Double = 20.0,
)