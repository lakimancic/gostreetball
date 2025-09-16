package com.example.gostreetball.data.repo

import android.location.Geocoder
import com.example.gostreetball.data.model.BoardType
import com.example.gostreetball.data.model.CourtType
import com.example.gostreetball.data.model.Court
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CourtRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val geocoder: Geocoder
) {
    suspend fun addCourt(
        name: String,
        type: CourtType,
        boardType: BoardType,
        image: ByteArray,
        location: GeoPoint
    ): Result<Unit> {
        return try {
            val (city, country) = getCityAndCountry(location.latitude, location.longitude) ?: ("" to "")
            val geoHash = GeoFireUtils.getGeoHashForLocation(GeoLocation(location.latitude, location.longitude))

            val courtsCollection = firestore.collection("courts")
            val newCourtRef = courtsCollection.document()

            val storageRef = storage.reference
                .child("courts/${newCourtRef.id}.jpg")

            storageRef.putBytes(image).await()
            val imageUrl = storageRef.downloadUrl.await().toString()

            val court = Court(
                id = newCourtRef.id,
                name = name,
                type = type,
                boardType = boardType,
                location = location,
                geoHash = geoHash,
                rating = 0.0,
                imageUrl = imageUrl,
                city = city,
                country = country
            )

            newCourtRef.set(court).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllCourts(): Result<List<Court>> {
        return runCatching {
            val snapshot = firestore.collection("courts")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Court::class.java)?.copy(id = doc.id)
            }
        }
    }

    suspend fun getCourtsInRadius(lat: Double, lng: Double, radius: Double): Result<List<Court>> {
        return runCatching {
            val center = GeoLocation(lat, lng)
            val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radius)
            val tasks = bounds.map { b ->
                firestore.collection("courts")
                    .orderBy("geoHash")
                    .startAt(b.startHash)
                    .endAt(b.endHash)
                    .get()
            }

            val snapshots = tasks.map { it.await() }
            val matchingDocs = mutableListOf<Court>()

            for (snap in snapshots) {
                for (doc in snap.documents) {
                    val court = doc.toObject(Court::class.java)?.copy(id = doc.id)
                    val courtLocation = court?.location
                    if (court != null && courtLocation != null) {
                        val distance = GeoFireUtils.getDistanceBetween(
                            GeoLocation(lat, lng),
                            GeoLocation(courtLocation.latitude, courtLocation.longitude)
                        )
                        if (distance <= radius) {
                            matchingDocs.add(court)
                        }
                    }
                }
            }

            matchingDocs
        }
    }

    suspend fun getCourt(courtId: String): Result<Court> {
        return runCatching {
            val docSnapshot = firestore.collection("courts")
                .document(courtId)
                .get()
                .await()

            val court = docSnapshot.toObject(Court::class.java)
                ?.copy(id = docSnapshot.id)
                ?: throw Exception("Court not found")

            court
        }
    }

    private suspend fun getCityAndCountry(lat: Double, lng: Double): Pair<String, String>? =
        withContext(Dispatchers.IO) {
            try {
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                val address = addresses?.firstOrNull()
                val city = address?.locality ?: address?.subAdminArea ?: ""
                val country = address?.countryName ?: ""
                city to country
            } catch (e: Exception) {
                null
            }
        }
}