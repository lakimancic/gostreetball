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

    fun observeLatestInvite(
        onChange: (GameInvite?) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        val firebaseUser = auth.currentUser ?: throw Exception("No logged in user")

        return firestore.collection("invites")
            .whereEqualTo("toUserId", firebaseUser.uid)
            .whereEqualTo("status", InviteStatus.PENDING.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e)
                    return@addSnapshotListener
                }

                val invite = snapshot?.documents
                    ?.firstOrNull()
                    ?.toObject(GameInvite::class.java)

                onChange(invite)
            }
    }

    suspend fun updateInviteStatus(inviteId: String, status: InviteStatus): Result<Unit> = runCatching {
        firestore.collection("invites").document(inviteId)
            .update("status", status)
            .await()
    }

    suspend fun cancelInvite(toUserId: String, gameId: String): Result<Unit> = runCatching {
        val snapshot = firestore.collection("invites")
            .whereEqualTo("toUserId", toUserId)
            .whereEqualTo("gameId", gameId)
            .get()
            .await()

        val batch = firestore.batch()

        snapshot.documents.forEach { doc ->
            batch.update(doc.reference, "status", InviteStatus.REJECTED)
        }

        batch.commit().await()
    }

    suspend fun createGame(game: Game): Result<String> = runCatching {
        val gameId = firestore.collection("games").document().id
        val gameWithId = game.copy(id = gameId)
        firestore.collection("games").document(gameId).set(gameWithId).await()
        gameId
    }

    suspend fun startGame(gameId: String, players: List<String>): Result<Unit> = runCatching {
        val currentUser = auth.currentUser ?: throw Exception("Not logged in")

        firestore.collection("games")
            .document(gameId)
            .update(
                mapOf(
                    "players" to players,
                    "judgeId" to currentUser.uid
                )
            )
            .await()
    }

    suspend fun getGame(gameId: String): Result<Game?> {
        return runCatching {
            val snapshot = firestore.collection("games")
                .document(gameId)
                .get()
                .await()

            snapshot.toObject(Game::class.java)
        }
    }

    fun observeFinishedInvitesForGame(
        gameId: String,
        onChange: (List<GameInvite>) -> Unit
    ): ListenerRegistration {
        return firestore.collection("invites")
            .whereEqualTo("gameId", gameId)
            .whereIn("status", listOf(InviteStatus.ACCEPTED.name, InviteStatus.REJECTED.name))
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val invites = snapshot.documents.mapNotNull { it.toObject(GameInvite::class.java) }
                    onChange(invites)
                }
            }
    }
}