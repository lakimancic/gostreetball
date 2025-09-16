package com.example.gostreetball.data.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    val uid: String = "",
    val username: String = "",
    val phoneNumber: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val profileImageUrl: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    val totalPoints: Long = 300L,
    val lastLocation: GeoPoint? = null,
    val lastGeoHash: String? = null,
    val lastLocationUpdate: Long = 0L,
    val currentCourt: String? = null,
    val gamesPlayer: Int = 0,

    // as judge
    val reviewCount: Int = 0,
    val rating: Double = 0.0,
)