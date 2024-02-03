package com.bitkid.itsfranking

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.commons.codec.language.bm.NameType
import org.apache.commons.codec.language.bm.PhoneticEngine
import org.apache.commons.codec.language.bm.RuleType
import java.io.File

class ITSFPlayers(rankings: List<Ranking>) {

    companion object {
        fun readFromFile(file: File): ITSFPlayers {
            val ranking = jacksonObjectMapper().readValue<List<Ranking>>(file)
            return ITSFPlayers(ranking)
        }
    }

    private val engine = PhoneticEngine(NameType.GENERIC, RuleType.APPROX, true)

    private data class CategoriesAndPlayer(val category: Category, val player: RankedPlayer)

    private val players: Map<String, ITSFPlayer>

    private val phoneticNames: Map<String, String>

    init {
        val playerPerLicense = rankings.flatMap { r -> r.database.values.map { CategoriesAndPlayer(r.category, it) } }
            .groupBy { it.player.licenseNumber }
        players = playerPerLicense.mapValues { playerEntry ->
            val licenseNumber = playerEntry.key
            val rankingsForPlayer = playerEntry.value
            val name = rankingsForPlayer.first().player.name
            val country = rankingsForPlayer.first().player.country
            val r = rankingsForPlayer.associate { it.category to ITSFRank(it.player.rank, it.player.points) }
            ITSFPlayer(licenseNumber, name, country, r)
        }
        phoneticNames = players.map {
            it
        }.associate {
            val encode = engine.encode(it.value.name)
            it.key to encode
        }
    }

    fun getSortedRanking(category: Category): List<ITSFPlayer> {
        return players.values.filter {
            it.rankings[category] != null
        }.sortedBy { it.rankings.getValue(category).rank }
    }

    fun getPlayer(licenseNumber: String): ITSFPlayer? {
        return players[licenseNumber]
    }

    fun find(search: String, searchForNameParts: Boolean = false): List<ITSFPlayer> {
        val res = findPlayer(search)
        val newRes = res.ifEmpty { findPlayer(search.split(" ").reversed().joinToString(" ")) }
        if (newRes.isEmpty() && searchForNameParts) {
            search.split(" ").forEach {
                val splitRes = findPlayer(it)
                if (splitRes.isNotEmpty())
                    return splitRes
            }
        }
        return newRes
    }

    private fun findPlayer(search: String): List<ITSFPlayer> {
        val normalSearch = players.filter { it.value.name.contains(search, true) }.map { it.value }
        return normalSearch.ifEmpty {
            phoneticSearch(search)
        }
    }

    private fun phoneticSearch(search: String): List<ITSFPlayer> {
        val enc = engine.encode(search).split("|")
        return phoneticNames.filter {
            matchesName(it.value, enc)
        }.map {
            players.getValue(it.key)
        }
    }

    fun matchesName(phoneticName: String, listWithPhoneticSubString: List<String>): Boolean {
        phoneticName.split("|").forEach { encName ->
            listWithPhoneticSubString.forEach { encSearch ->
                if (encName.contains(encSearch, true)) {
                    return true
                }
            }
        }
        return false
    }
}