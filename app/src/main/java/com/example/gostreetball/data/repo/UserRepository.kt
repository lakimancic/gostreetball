package com.example.gostreetball.data.repo

import com.example.gostreetball.data.model.User
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
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

    suspend fun getScoreboard(): Result<List<User>> {
        return runCatching {
            val snapshot = firestore.collection("users")
                .orderBy("totalPoints", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { it.toObject(User::class.java) }
        }
    }
}