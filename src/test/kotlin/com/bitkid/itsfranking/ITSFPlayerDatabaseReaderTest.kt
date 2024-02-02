package com.bitkid.itsfranking

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.runBlocking
import org.apache.commons.codec.language.bm.NameType
import org.apache.commons.codec.language.bm.PhoneticEngine
import org.apache.commons.codec.language.bm.RuleType
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isTrue
import java.io.File

class ITSFPlayerDatabaseReaderTest {

    @Disabled
    @Test
    fun `collects player data`() {
        val playerDatabase = runBlocking {
            ITSFPlayerDatabaseReader(topXPlayers = 2000).readRankings()
        }
        jacksonObjectMapper().writeValue(File("itsfFullRankings_2023.json"), playerDatabase)
    }

    @Test
    fun `parse page`() {
        val resource = ITSFPlayerDatabaseReaderTest::class.java.getResource("/ranking_singles_male.html")
        val data = File(resource!!.toURI()).readText()
        ITSFPlayerDatabaseReader(topXPlayers = 2000).getPlayersMap(CategoryAndPage(Categories.openSingles, data, "url"))
    }

    @Test
    fun `parse rankings`() {
        expectThat(players.players["84000895"]!!.name).isEqualTo("SPREDEMAN Tony")
        expectThat(players.find("sprede")).hasSize(2)

        val varos = "70300218"
        val simon = players.players[varos]!!
        val engine = PhoneticEngine(NameType.GENERIC, RuleType.APPROX, true)

        expectThat(players.matchesName(engine.encode(simon.name), engine.encode("varos").split("|"))).isTrue()
        expectThat(players.find("varos").size).isGreaterThan(1)
    }
}