package com.example.gostreetball.data.repo

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.gostreetball.data.model.Review
import com.example.gostreetball.data.model.User
import com.example.gostreetball.utils.imageBitmapToByteArray
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
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

    suspend fun updateProfilePicture(imageBitmap: Bitmap): Result<String> {
        return runCatching {
            val firebaseUser = auth.currentUser ?: throw Exception("No logged in user")
            val bytes = imageBitmapToByteArray(imageBitmap.asImageBitmap())
            val storageRef = storage.reference.child("users/${firebaseUser.uid}/profile.jpg")
            storageRef.putBytes(bytes).await()

            val imageUrl = storageRef.downloadUrl.await().toString()

            val userRef = firestore.collection("users").document(firebaseUser.uid)
            userRef.update("profileImageUrl", imageUrl).await()

            imageUrl
        }
    }

    suspend fun getProfilePictureUrl(uid: String): Result<String> {
        return runCatching {
            val userRef = firestore.collection("users").document(uid)
            val snapshot = userRef.get().await()

            if (!snapshot.exists()) throw Exception("User not found")

            snapshot.getString("profileImageUrl") ?: ""
        }
    }

    suspend fun getUsersInRadius(lat: Double, lng: Double, radius: Double): Result<List<User>> {
        return runCatching {
            val currentTime = System.currentTimeMillis()
            val fiveMinutesMillis = 5 * 60 * 1000
            val center = GeoLocation(lat, lng)
            val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radius)

            val tasks = bounds.map { b ->
                firestore.collection("users")
                    .orderBy("lastGeoHash")
                    .startAt(b.startHash)
                    .endAt(b.endHash)
                    .get()
            }

            val snapshots = tasks.map { it.await() }
            val matchingUsers = mutableListOf<User>()

            for (snap in snapshots) {
                for (doc in snap.documents) {
                    val user = doc.toObject(User::class.java)?.copy(uid = doc.id)
                    val userLocation = user?.lastLocation
                    val lastUpdate = user?.lastLocationUpdate ?: 0L

                    if (user != null && userLocation != null && user.uid != auth.currentUser?.uid && (currentTime - lastUpdate) <= fiveMinutesMillis) {
                        val distance = GeoFireUtils.getDistanceBetween(
                            GeoLocation(lat, lng),
                            GeoLocation(userLocation.latitude, userLocation.longitude)
                        )
                        if (distance <= radius) {
                            matchingUsers.add(user)
                        }
                    }
                }
            }

            matchingUsers
        }
    }

    suspend fun getCurrentUser(): Result<User> = runCatching {
        val firebaseUser = auth.currentUser ?: throw Exception("No logged-in user found")
        val userRef = firestore.collection("users").document(firebaseUser.uid)
        val snapshot = userRef.get().await()
        if (!snapshot.exists()) throw Exception("User document does not exist")
        snapshot.toObject(User::class.java)?.copy(uid = snapshot.id)
            ?: throw Exception("Failed to parse user data")
    }

    suspend fun clearCurrentUserCourtIfInvalid(activeCourtIds: Set<String>): Result<Unit> {
        return runCatching {
            val firebaseUser = auth.currentUser ?: throw Exception("No logged-in user")
            val userRef = firestore.collection("users").document(firebaseUser.uid)

            val snapshot = userRef.get().await()
            val currentCourt = snapshot.getString("currentCourt")

            if (!currentCourt.isNullOrEmpty() && currentCourt !in activeCourtIds) {
                userRef.update("currentCourt", null).await()
            }
        }
    }

    suspend fun getUserWithRankAndReview(
        userId: String,
    ): Result<Triple<User, Boolean, Int?>> {
        return runCatching {
            val userSnapshot = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val user = userSnapshot.toObject(User::class.java)
                ?.copy(uid = userSnapshot.id)
                ?: throw Exception("User not found")

            val authUserId = auth.currentUser?.uid
            val hasReviewed = if (authUserId != null) {
                val reviewQuery = firestore.collection("reviews")
                    .whereEqualTo("forCourt", false)
                    .whereEqualTo("itemId", userId)
                    .whereEqualTo("userId", authUserId)
                    .get()
                    .await()

                !reviewQuery.isEmpty
            } else {
                false
            }

            val allUsersSnapshot = firestore.collection("users")
                .orderBy("totalPoints", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val allUsers = allUsersSnapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(uid = doc.id)
            }

            val rank = allUsers.indexOfFirst { it.uid == userId }
                .takeIf { it >= 0 }
                ?.plus(1)

            Triple(user, hasReviewed, rank)
        }
    }

    suspend fun addReview(
        userId: String,
        stars: Int,
        text: String
    ): Result<Unit> {
        return runCatching {
            val userRef = firestore.collection("users").document(userId)
            val userSnapshot = userRef.get().await()
            val curUserId = auth.currentUser?.uid

            if (!userSnapshot.exists()) throw Exception("User not found")
            if (curUserId == null) throw Exception("Current User not found")

            val user = userSnapshot.toObject(User::class.java) ?: throw Exception("User data invalid")

            val reviewsRef = firestore.collection("reviews")
            val existingReviewQuery = reviewsRef
                .whereEqualTo("itemId", userId)
                .whereEqualTo("userId", curUserId)
                .whereEqualTo("forCourt", false)
                .get()
                .await()

            val oldStars = if (!existingReviewQuery.isEmpty) {
                val doc = existingReviewQuery.documents.first()
                doc.reference.update(
                    mapOf(
                        "stars" to stars,
                        "text" to text
                    )
                ).await()
                (doc.getLong("stars") ?: 0L).toInt()
            } else {
                val review = Review(
                    stars = stars,
                    text = text,
                    isForCourt = false,
                    userId = curUserId,
                    itemId = userId
                )
                reviewsRef.add(review).await()
                null
            }

            val newReviewCount = if (oldStars == null) user.reviewCount + 1 else user.reviewCount
            val totalStars = user.rating * user.reviewCount - (oldStars ?: 0) + stars
            val newRating = if (newReviewCount > 0) totalStars / newReviewCount else 0.0

            userRef.update(
                mapOf(
                    "reviewCount" to newReviewCount,
                    "rating" to newRating
                )
            ).await()
        }
    }

    suspend fun getUsersForIds(ids: List<String>): Result<List<User>> {
        return runCatching {
            if (ids.isEmpty()) return@runCatching emptyList<User>()

            val snapshot = firestore.collection("users")
                .whereIn(FieldPath.documentId(), ids)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(uid = doc.id)
            }
        }
    }

    suspend fun getReview(itemId: String): Result<Review?> {
        return runCatching {
            val snapshot = firestore.collection("reviews")
                .whereEqualTo("itemId", itemId)
                .whereEqualTo("forCourt", false)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(Review::class.java)
        }
    }

    suspend fun getReviews(userId: String): Result<List<Review>> {
        return runCatching {
            val snapshot = firestore.collection("reviews")
                .whereEqualTo("itemId", userId)
                .whereEqualTo("forCourt", false)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Review::class.java)
            }
        }
    }

    suspend fun getActiveUsersInCourt(courtId: String): Result<List<User>> = runCatching {
        val currentUser = auth.currentUser ?: throw Exception("Not logged in")
        val fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000

        val snapshot = firestore.collection("users")
            .whereEqualTo("currentCourt", courtId)
            .whereGreaterThanOrEqualTo("lastLocationUpdate", fiveMinutesAgo)
            .get()
            .await()

        snapshot.documents.mapNotNull { doc ->
            doc.toObject(User::class.java)?.takeIf { it.uid != currentUser.uid }
        }
    }

    suspend fun joinCourt(courtId: String): Result<Unit> {
        return runCatching {
            val userRef = firestore.collection("users").document(auth.currentUser?.uid ?: "")
            userRef.update(
                mapOf(
                    "currentCourt" to courtId,
                )
            ).await()
        }
    }

    suspend fun leaveCourt(): Result<Unit> {
        return runCatching {
            val userRef = firestore.collection("users").document(auth.currentUser?.uid ?: "")
            userRef.update(
                mapOf(
                    "currentCourt" to null,
                )
            ).await()
        }
    }
}