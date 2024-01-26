package com.bitkid.foostats.ranking

import org.apache.commons.codec.language.bm.NameType
import org.apache.commons.codec.language.bm.PhoneticEngine
import org.apache.commons.codec.language.bm.RuleType

data class ITSFRank(val rank: Int, val points: Int)

data class ITSFPlayer(
    val licenseNumber: String,
    val name: String,
    val country: String,
    val rankings: Map<Category, ITSFRank>
)

class ITSFPlayers(rankings: List<Ranking>) {

    private val engine = PhoneticEngine(NameType.GENERIC, RuleType.APPROX, true)

    data class CategoriesAndPlayer(val category: Category, val player: RankedPlayer)

    val players: Map<String, ITSFPlayer>

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

    fun getRanking(category: Category): List<ITSFPlayer> {
        return players.values.filter {
            it.rankings[category] != null
        }.sortedBy { it.rankings.getValue(category).rank }
    }

    fun find(search: String): List<ITSFPlayer> {
        val normalSearch = players.filter { it.value.name.contains(search, true) }.map { it.value }
        if (normalSearch.isEmpty()) {
            val enc = engine.encode(search).split("|")
            return phoneticNames.filter {
                matchesName(it.value, enc)
            }.map {
                players.getValue(it.key)
            }
        } else {
            return normalSearch
        }
    }

    fun matchesName(name: String, enc: List<String>): Boolean {
        name.split("|").forEach { encName ->
            enc.forEach { encSearch ->
                if (encName.contains(encSearch, true)) {
                    return true
                }
            }
        }
        return false
    }
}