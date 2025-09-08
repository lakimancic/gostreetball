package com.example.gostreetball.data.repo

import android.content.Context
import com.example.gostreetball.data.model.User
import com.example.gostreetball.data.model.auth.LoginData
import com.example.gostreetball.data.model.auth.RegisterData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun login(data: LoginData): Result<FirebaseUser> {
        return runCatching {
            val result = auth.signInWithEmailAndPassword("${data.username}@fake.com", data.password).await()
            result.user ?: throw Exception("Login failed: user is null")
        }
    }

    suspend fun register(data: RegisterData): Result<FirebaseUser> {
        return runCatching {
            val result = auth.createUserWithEmailAndPassword("${data.username}@fake.com", data.password).await()
            val firebaseUser = result.user ?: throw Exception("Something went wrong creating user")

            val user = User(
                uid = firebaseUser.uid,
                username = data.username,
                firstName = data.firstName,
                lastName = data.lastName,
                phoneNumber = data.phone
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