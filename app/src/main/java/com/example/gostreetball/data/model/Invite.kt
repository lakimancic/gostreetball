package com.example.gostreetball.data.model

enum class InviteStatus { PENDING, ACCEPTED, REJECTED }

data class GameInvite(
    val id: String = "",
    val gameType: GameType = GameType.ONE_VS_ONE,
    val gameId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val status: InviteStatus = InviteStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)