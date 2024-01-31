package com.bitkid.itsfranking

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File


data class RankedPlayer(
    val name: String,
    val licenseNumber: String,
    val country: String,
    val rank: Int,
    val points: Int
)

data class CategoryAndPage(val category: Category, val page: String, val url: String)

data class Ranking(val category: Category, val database: Map<String, RankedPlayer>)

class ITSFPlayerDatabaseReader(private val topXPlayers: Int = 30, private val tour: String = "2023") {

    private suspend fun readCategoryPage(categories: List<Category>): List<CategoryAndPage> {
        val data = HttpClient(CIO).use { client ->
            coroutineScope {
                categories.map {
                    async {
                        val urlString = createUrl(it.targetAudience, it.system)
                        val page = client.get(urlString).readBytes().toString(Charsets.UTF_8)
                        CategoryAndPage(it, page, urlString)
                    }
                }.awaitAll()
            }
        }
        return data
    }

    suspend fun readRankings(): List<Ranking> {
        val categoryData = readCategoryPage(Categories.all)
        return categoryData.map {
            try {
                val data = getPlayersMap(it)
                Ranking(it.category, data)
            } catch (e: Exception) {
                throw RuntimeException("$it", e)
            }
        }
    }

    fun readTestRankings(): List<Ranking> {
        val resource = ITSFPlayerDatabaseReader::class.java.getResource("/itsfFullRankings_2023.json")
        val data = File(resource!!.toURI())
        return jacksonObjectMapper().readValue<List<Ranking>>(data)
    }

    fun getPlayersMap(it: CategoryAndPage): Map<String, RankedPlayer> {
        val lines = getLines(it.page)
        val filteredLines = lines.filter { line -> isRankingLine(line) }
        return filteredLines
            .flatMap { line -> getPlayers(line) }
            .associateBy { player -> player.licenseNumber }
    }

    private fun createUrl(competition: String, system: Int) =
        "https://www.tablesoccer.org/page/rankings?category=$competition&tour=$tour&vues=$topXPlayers&p=0&system=$system"

    private fun getLines(responseAsString: String) = responseAsString.split("\r\n")

    private fun isRankingLine(it: String) =
        it.contains("rnk") && it.contains("pntz") && it.contains("numlic") && it.contains("&system")

    private fun getPlayers(line: String): List<RankedPlayer> {
        val allRankings = line.trim()
        return allRankings.split("</table></div>").filter { it.contains("placenum") }.map { playerLine ->
            val cleanedPlayersLine = playerLine.substringAfter("id=\"place")
            val licenseNumber = cleanedPlayersLine.substringAfter("&numlic=").substringBefore("&system")
            val country = cleanedPlayersLine.substringAfter(".png\" /> ").substringBefore("<")
            val rank = cleanedPlayersLine.substringAfter("<div class=\"rnk\">").substringBefore("<").toInt()
            val points = cleanedPlayersLine.substringAfter("<div class=\"pntz\">").substringBefore("<").toInt()
            val name = cleanedPlayersLine.substringAfter("class=\"lname\">").substringBefore("</div>")
            RankedPlayer(name, licenseNumber, country, rank, points)
        }
    }
}