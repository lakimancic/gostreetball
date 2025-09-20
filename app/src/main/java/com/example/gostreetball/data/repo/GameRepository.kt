package com.example.gostreetball.data.repo

import android.util.Log
import com.example.gostreetball.data.model.Game
import com.example.gostreetball.data.model.GameInvite
import com.example.gostreetball.data.model.GameType
import com.example.gostreetball.data.model.InviteStatus
import com.example.gostreetball.data.model.User
import com.example.gostreetball.utils.EloSystem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.Query
import javax.inject.Inject

class GameRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val eloSystem: EloSystem
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
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e)
                    return@addSnapshotListener
                }

                val invite = snapshot?.documents
                    ?.mapNotNull { it.toObject(GameInvite::class.java) }
                    ?.maxByOrNull { it.createdAt }

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

    suspend fun updateScore(
        game: Game,
        playerOrders: List<User>,
        winnerIndex: Int? = null
    ): Result<Unit> = runCatching {
        val courtCoefficient = playerOrders.firstOrNull()?.currentCourt?.let { courtId ->
            firestore.collection("courts")
                .document(courtId)
                .get()
                .await()
                .getDouble("coefficient") ?: 20.0
        } ?: 20.0

        when (game.type) {
            GameType.SEVEN_UP, GameType.AROUND_THE_WORLD -> {
                val ratings = playerOrders.map { it.totalPoints.toDouble() }
                val ranks = List(playerOrders.size) { i -> i + 1 }
                val deltas = eloSystem.updateRatingsMultiplayer(
                    ratings,
                    ranks,
                    kBase = courtCoefficient
                )

                playerOrders.forEachIndexed { i, user ->
                    firestore.collection("users")
                        .document(user.uid)
                        .update(
                            mapOf(
                                "totalPoints" to (deltas[i].toLong()),
                                "gamesPlayer" to (user.gamesPlayer + 1)
                            )
                        ).await()
                }
            }

            GameType.ONE_VS_ONE, GameType.THREE_X_THREE -> {
                val teamSize = if (game.type == GameType.ONE_VS_ONE) 1 else 3
                val teamA = playerOrders.take(teamSize)
                val teamB = playerOrders.drop(teamSize).take(teamSize)

                val ratingsA = teamA.map { it.totalPoints.toDouble() }
                val ratingsB = teamB.map { it.totalPoints.toDouble() }

                val winnerTeam = if (winnerIndex != null && winnerIndex < teamA.size) 0 else 1

                val (newA, newB) = eloSystem.updateTeamMatch(
                    ratingsA,
                    ratingsB,
                    winner = winnerTeam,
                    kBase = courtCoefficient,
                    splitChange = false
                )

                teamA.forEachIndexed { i, user ->
                    firestore.collection("users")
                        .document(user.uid)
                        .update(
                            mapOf(
                                "totalPoints" to (newA[i].toLong()),
                                "gamesPlayer" to (user.gamesPlayer + 1)
                            )
                        ).await()
                }
                teamB.forEachIndexed { i, user ->
                    firestore.collection("users")
                        .document(user.uid)
                        .update(
                            mapOf(
                                "totalPoints" to (newB[i].toLong()),
                                "gamesPlayer" to (user.gamesPlayer + 1)
                            )
                        ).await()
                }
            }
        }

        firestore.collection("games")
            .document(game.id)
            .set(game.copy(winner = winnerIndex ?: 0))
            .await()
    }
}