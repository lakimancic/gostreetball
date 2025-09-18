package com.example.gostreetball.data.repo

import com.example.gostreetball.data.model.User
import com.example.gostreetball.data.model.auth.LoginData
import com.example.gostreetball.data.model.auth.RegisterData
import com.example.gostreetball.utils.imageBitmapToByteArray
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    suspend fun login(data: LoginData): Result<FirebaseUser> {
        return runCatching {
            val result = auth.signInWithEmailAndPassword(
                "${data.username}@fake.com",
                data.password
            ).await()

            val firebaseUser = result.user ?: throw Exception("Login failed: user is null")

            firestore.collection("users").document(firebaseUser.uid)
                .update("currentCourt", null)
                .await()

            firebaseUser
        }
    }

    suspend fun register(data: RegisterData): Result<FirebaseUser> {
        return runCatching {
            val result = auth.createUserWithEmailAndPassword("${data.username}@fake.com", data.password).await()
            val firebaseUser = result.user ?: throw Exception("Something went wrong creating user")

            var imageUrl = ""
            data.profileImage?.let { imageBitmap ->
                val bytes = imageBitmapToByteArray(imageBitmap)
                val storageRef = storage.reference.child("users/${firebaseUser.uid}/profile.jpg")
                storageRef.putBytes(bytes).await()
                imageUrl = storageRef.downloadUrl.await().toString()
            }

            val user = User(
                uid = firebaseUser.uid,
                username = data.username,
                firstName = data.firstName,
                lastName = data.lastName,
                phoneNumber = data.phone,
                profileImageUrl = imageUrl
            )

            saveUserToFireStore(user)

            firebaseUser
        }
    }

    private suspend fun saveUserToFireStore(user: User) {
        firestore.collection("users")
            .document(user.uid)
            .set(user)
            .await()
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun logout() = auth.signOut()
}