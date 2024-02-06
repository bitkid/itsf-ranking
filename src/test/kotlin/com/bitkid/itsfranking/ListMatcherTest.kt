package com.bitkid.itsfranking

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import java.io.File
import java.nio.charset.Charset

val players by lazy { ITSFPlayers.readFromFile(File(ITSFPlayerDatabaseReaderTest::class.java.getResource("/itsfFullRankings_2023.json")!!.toURI())) }

class ListMatcherTest {

    private val singles = File(ITSFPlayerDatabaseReaderTest::class.java.getResource("/test_tournament.csv")!!.toURI())
    private val mixed = File(ITSFPlayerDatabaseReaderTest::class.java.getResource("/test_tournament_mixed.csv")!!.toURI())

    @Test
    fun singles() {
        val matcher = ListMatcher(players)
        val result = matcher.matchPlayer(singles, Charset.forName("WINDOWS-1252"), Categories.openSingles)
        expectThat(result.filter { it.results.size > 1 }).hasSize(3)
        expectThat(result.filter { it.results.isEmpty() }).hasSize(1)
    }

    @Test
    fun `fails with wrong input`() {
        val matcher = ListMatcher(players)
        expectThrows<RuntimeException> { matcher.matchPlayer(mixed, Charset.forName("WINDOWS-1252"), Categories.openDoubles) }
            .get { message }.isEqualTo("expecting a singles category")

        expectThrows<RuntimeException> { matcher.matchPlayer(mixed, Charset.forName("UTF-8"), Categories.openSingles) }
            .get { message }.isEqualTo("not expecting multiple players per line")

        expectThrows<RuntimeException> { matcher.matchTeam(singles, Charset.forName("WINDOWS-1252"), Categories.openSingles) }
            .get { message }.isEqualTo("expecting a doubles category")

        expectThrows<RuntimeException> { matcher.matchTeam(singles, Charset.forName("WINDOWS-1252"), Categories.openDoubles) }
            .get { message }.isEqualTo("expect exactly 2 players per line")
    }

    @Test
    fun mixed() {
        val matcher = ListMatcher(players)
        val result = matcher.matchTeam(mixed, Charsets.UTF_8, Categories.mixedDoubles)
        expectThat(result.filter { it.player1.results.isEmpty() || it.player2.results.isEmpty() }).hasSize(0)
        expectThat(result.filter { it.player1.results.size > 1 || it.player2.results.size > 1 }).hasSize(3)
    }
}