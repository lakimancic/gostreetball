package com.example.gostreetball.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class GameType {
    ONE_VS_ONE,
    THREE_X_THREE,
    SEVEN_UP,
    AROUND_THE_WORLD
}

data class Game (
    val id: String = "",
    val type: GameType = GameType.ONE_VS_ONE,
    val createdAt: Long = System.currentTimeMillis(),
    val judgeId: String = "",
    val players: List<String> = emptyList(),
    val winner: Int = -1,

    val settings: GameSettings = GameSettings()
)

data class GameSettings (
    val targetPoints: Int = 0,
    val winByTwo: Boolean = false,
    val makeItTakeIt: Boolean = false,
    val missCount: Boolean = true,
    val longRoute: Boolean = true
)