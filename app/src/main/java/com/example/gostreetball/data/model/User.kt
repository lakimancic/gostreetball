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
    val monthlyPoints: Map<String, Long> = emptyMap(),
    val lastLocation: GeoPoint? = null,
    val lastLocationUpdate: Long = 0L,

    // as judge
    val reviews: List<Review> = emptyList(),
)