package com.example.gostreetball.data.repo

import com.example.gostreetball.data.model.CourtType
import com.example.gostreetball.data.model.Court
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CourtRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun addCourt(
        name: String,
        type: CourtType
    ): Result<Unit> {
        return try {
            val courtsCollection = firestore.collection("courts")

            val newCourtRef = courtsCollection.document()

            val court = Court(
                id = newCourtRef.id,
                name = name,
                type = type,
                location = null,
                rating = 0.0,
                reviews = emptyList(),
                games = emptyList(),
                imageUrl = ""
            )

            newCourtRef.set(court).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}