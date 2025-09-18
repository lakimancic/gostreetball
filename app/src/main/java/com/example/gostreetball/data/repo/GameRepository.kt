package com.example.gostreetball.data.repo

import com.example.gostreetball.data.model.Game
import com.example.gostreetball.data.model.GameInvite
import com.example.gostreetball.data.model.GameType
import com.example.gostreetball.data.model.InviteStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.Query
import javax.inject.Inject

class GameRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun sendInvite(toUserId: String, gameId: String, gameType: GameType): Result<String> = runCatching {
        val currentUser = auth.currentUser ?: throw Exception("Not logged in")
        val invite = GameInvite(
            id = firestore.collection("invites").document().id,
            fromUserId = currentUser.uid,
            toUserId = toUserId,
            gameId = gameId,
            gameType = gameType
        )
        firestore.collection("invites").document(invite.id).set(invite).await()
        invite.id
    }

    suspend fun getLatestInvite(): Result<GameInvite?> = runCatching {
        val firebaseUser = auth.currentUser ?: throw Exception("No logged in user")

        val snapshot = firestore.collection("invites")
            .whereEqualTo("toUserId", firebaseUser.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        snapshot.documents.firstOrNull()?.toObject(GameInvite::class.java)
    }

    suspend fun updateInviteStatus(inviteId: String, status: InviteStatus): Result<Unit> = runCatching {
        firestore.collection("invites").document(inviteId)
            .update("status", status)
            .await()
    }

    suspend fun createGame(game: Game): Result<String> = runCatching {
        val gameId = firestore.collection("games").document().id
        val gameWithId = game.copy(id = gameId)
        firestore.collection("games").document(gameId).set(gameWithId).await()
        gameId
    }

    fun observeAcceptedInvitesForGame(
        gameId: String,
        onChange: (List<GameInvite>) -> Unit
    ): ListenerRegistration {
        return firestore.collection("invites")
            .whereEqualTo("gameId", gameId)
            .whereEqualTo("status", InviteStatus.ACCEPTED)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val invites = snapshot.documents.mapNotNull { it.toObject(GameInvite::class.java) }
                    onChange(invites)
                }
            }
    }
}