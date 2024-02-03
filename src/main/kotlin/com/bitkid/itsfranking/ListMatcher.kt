package com.bitkid.itsfranking

import java.io.File
import java.nio.charset.Charset

class ListMatcher(private val players: ITSFPlayers) {

    private fun getFileAsListOfString(file: File, charset: Charset): List<String> {
        val lines = file.readLines(charset)
        val data = lines.takeLast(lines.size - 1)
        return data
    }

    fun matchTeam(file: File, charset: Charset, category: Category): List<TwoPlayersWithResults> {
        require(category.type == CompetitionType.DOUBLES) { "expecting a doubles category" }
        val data = getFileAsListOfString(file, charset)
        return data.map {
            val playerNames = it.split(";")
            require(playerNames.size == 2) { "expect exactly 2 players per line" }

            val player1Name = playerNames[0].trim()
            val results = players.find(player1Name, true)
            val player1 = PlayerNameWithResults(playerNames[0], results)

            val player2Name = playerNames[1].trim()
            val results2 = players.find(player2Name, true)
            val player2 = PlayerNameWithResults(playerNames[1], results2)

            TwoPlayersWithResults(player1, player2)
        }
    }

    fun matchPlayer(file: File, charset: Charset, category: Category): List<PlayerNameWithResults> {
        require(category.type == CompetitionType.SINGLES) { "expecting a singles category" }
        val data = getFileAsListOfString(file, charset)
        return data.map {
            require(!it.contains(";")) { "not expecting multiple players per line" }
            PlayerNameWithResults(it, players.find(it, true))
        }
    }

}