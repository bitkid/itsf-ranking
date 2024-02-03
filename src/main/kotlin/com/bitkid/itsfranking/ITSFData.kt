package com.bitkid.itsfranking

data class ITSFRank(val rank: Int, val points: Int)

data class ITSFPlayer(
    val licenseNumber: String,
    val name: String,
    val country: String,
    val rankings: Map<Category, ITSFRank>
) {
    fun hasFemaleRanking(): Boolean {
        return rankings[Categories.womenDoubles] != null || rankings[Categories.womenSingles] != null
    }
}

data class PlayerNameWithResults(val playerName: String, val results: List<ITSFPlayer>)
data class TwoPlayersWithResults(val player1: PlayerNameWithResults, val player2: PlayerNameWithResults)


