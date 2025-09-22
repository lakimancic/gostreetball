package com.example.gostreetball.utils

import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

@Singleton
class EloSystem {
    companion object {
        const val MIN_RATING = 300.0
        const val K_BASE_MULTIPLIER = 5.0
    }

    private fun pWin(ra: Double, rb: Double): Double {
        return 1.0 / (1.0 + 10.0.pow((rb - ra) / 400.0))
    }

    private fun expectedScore(rating: Double, others: List<Double>): Double {
        return others.map { pWin(rating, it) }.average()
    }

    private fun actualScore(rank: Int, nPlayers: Int): Double {
        return (nPlayers - rank).toDouble() / (nPlayers - 1)
    }

    fun updateRatingsMultiplayer(
        ratings: List<Double>,
        ranks: List<Int>,
        kBase: Double,
    ): List<Double> {
        val n = ratings.size
        val kEff = kBase * K_BASE_MULTIPLIER * sqrt((n - 1).toDouble())
        return ratings.mapIndexed { i, ri ->
            val others = ratings.filterIndexed { j, _ -> j != i }
            val ei = expectedScore(ri, others)
            val si = actualScore(ranks[i], n)
            maxOf(ri + kEff * (si - ei), MIN_RATING)
        }
    }

    fun updateTeamMatch(
        ratingsTeamA: List<Double>,
        ratingsTeamB: List<Double>,
        winner: Int = 0,
        kBase: Double,
        splitChange: Boolean = false
    ): Pair<List<Double>, List<Double>> {
        val teamARating = ratingsTeamA.average()
        val teamBRating = ratingsTeamB.average()

        val pA = pWin(teamARating, teamBRating)
        val pB = 1.0 - pA

        val (sA, sB) = if (winner == 0) 1.0 to 0.0 else 0.0 to 1.0

        var dA = kBase * K_BASE_MULTIPLIER * (sA - pA)
        var dB = kBase * K_BASE_MULTIPLIER * (sB - pB)

        if (splitChange) {
            dA /= ratingsTeamA.size
            dB /= ratingsTeamB.size
        }

        val newTeamA = ratingsTeamA.map { maxOf(it + dA, MIN_RATING) }
        val newTeamB = ratingsTeamB.map { maxOf(it + dB, MIN_RATING) }

        return newTeamA to newTeamB
    }
}
