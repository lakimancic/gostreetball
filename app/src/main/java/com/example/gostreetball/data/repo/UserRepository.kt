package com.example.gostreetball.data.repo

import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun updateLocation(location: LatLng) : Result<Long> {
        return runCatching {
            val currentUser = auth.currentUser ?: throw Exception("No current user")
            val currentTime = System.currentTimeMillis()
            val geoHash = GeoFireUtils.getGeoHashForLocation(GeoLocation(location.latitude, location.longitude))

            val userRef = firestore.collection("users").document(currentUser.uid)
            val locationData = mapOf(
                "lastLocation" to GeoPoint(location.latitude, location.longitude),
                "lastLocationUpdate" to currentTime,
                "lastGeoHash" to geoHash
            )

            userRef.update(locationData).await()
            currentTime
        }
    }
}