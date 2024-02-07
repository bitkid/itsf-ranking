package com.bitkid.itsfranking

import com.bitkid.itsfranking.ui.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import java.awt.Color
import java.awt.event.ActionEvent
import java.io.File
import java.nio.charset.Charset
import javax.swing.*


@OptIn(DelicateCoroutinesApi::class)
fun showLoadingDialog(): JDialog {
    val jDialog = JDialog(ITSFRankingApp.jFrame, "loading", true)
    jDialog.contentPane.layout = MigLayout()
    jDialog.isUndecorated = true
    jDialog.contentPane.add(JLabel(" LOADING ...").apply {
        border = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.black)
    }, "width :100:")
    jDialog.defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
    jDialog.pack()
    jDialog.setLocationRelativeTo(ITSFRankingApp.jFrame)
    GlobalScope.launch(Dispatchers.Swing) {
        jDialog.isVisible = true
    }
    return jDialog
}

@DelicateCoroutinesApi
object ITSFRankingApp {
    private lateinit var itsfPlayers: ITSFPlayers
    lateinit var jFrame: JFrame

    val version = getVersionFromJarFile()

    private val playerNameField = JTextField(50)
    private val itsfNoField = JTextField(50)

    private val searchResultTable = JTable(emptySearchResultModel()).apply {
        cellSelectionEnabled = true
        autoCreateRowSorter = true
    }

    private val csvListTable = JTable(emptyCsvListModelSingles()).apply {
        cellSelectionEnabled = true
        autoCreateRowSorter = true
    }

    private val rankingTable = JTable(emptyRankingModel()).apply {
        cellSelectionEnabled = true
        autoCreateRowSorter = true
    }

    private val tabbedPane = JTabbedPane()
    private fun emptySearchResultModel() = ResultTableModel(listOf("itsf no.", "name", "country") + Categories.all.map { it.targetAudience })
    private fun emptyRankingModel() = ResultTableModel(listOf("rank", "name", "country", "itsf no.", "points"))
    private fun emptyCsvListModelSingles() = ResultTableModel(listOf("player", "itsf name", "country", "points", "rank", "status"), true)
    private fun emptyCsvListModelDoubles() = DoublesListModel(
        listOf(
            "player1",
            "itsf name 1",
            "player2",
            "itsf name 2",
            "combined",
            "p1 country",
            "p1 points",
            "p1 rank",
            "p1 status",
            "p2 country",
            "p2 points",
            "p2 rank",
            "p2 status"
        )
    )

    private fun showRanking(category: String) {
        if (checkITSFDataLoaded()) {
            val cat = Categories.all.single { it.targetAudience == category }
            val players = itsfPlayers.getSortedRanking(cat)
            val m = emptyRankingModel()
            players.forEach {
                val itsfRank = it.rankings.getValue(cat)
                listOf("rank", "name", "country", "itsf no.", "points")
                val props = listOf(itsfRank.rank, it.name, it.country, it.licenseNumber, itsfRank.points)
                m.addRow(props)
            }
            rankingTable.rowSorter = null
            rankingTable.model = m
            tabbedPane.selectedIndex = 2
        }
    }

    private fun checkITSFDataLoaded(): Boolean {
        if (!this::itsfPlayers.isInitialized) {
            JOptionPane.showMessageDialog(jFrame, "Load rankings first!", "Info", JOptionPane.INFORMATION_MESSAGE)
            return false
        }
        return true
    }

    private fun modelWithPlayers(players: Collection<ITSFPlayer>): ResultTableModel {
        val model = emptySearchResultModel()
        players.forEach { addPlayerToSearchResultModel(model, it) }
        return model
    }

    private suspend fun loadITSFDATA(s: String) {
        // val rankings = ITSFPlayerDatabaseReader(topXPlayers = 2000).readTestRankings()
        val rankings = ITSFPlayerDatabaseReader(topXPlayers = 2000, tour = s).readRankings()
        itsfPlayers = ITSFPlayers(rankings)
    }

    private fun searchForLicenseNumber() {
        if (checkITSFDataLoaded()) {
            val text = itsfNoField.text
            if (!text.isNullOrBlank()) {
                val player = itsfPlayers.getPlayer(text)
                searchResultTable.rowSorter = null
                if (player == null)
                    searchResultTable.model = emptySearchResultModel()
                else
                    searchResultTable.model = modelWithPlayers(listOf(player))
                tabbedPane.selectedIndex = 0
            }
        }
    }

    private fun searchForName() {
        if (checkITSFDataLoaded()) {
            val text = playerNameField.text
            if (!text.isNullOrBlank()) {
                val player = itsfPlayers.find(text, true)
                searchResultTable.rowSorter = null
                searchResultTable.model = modelWithPlayers(player)
                tabbedPane.selectedIndex = 0
            }
        }
    }


    private fun loadCsvFile(file: File, charset: Charset, category: Category) {
        val listMatcher = ListMatcher(itsfPlayers)
        val model = if (category.type == CompetitionType.SINGLES) {
            val model = emptyCsvListModelSingles()
            listMatcher.matchPlayer(file, charset, category).forEach {
                when (it.results.size) {
                    0 -> model.addRow(listOf(it.playerName, null, null, null, null, "NOT_FOUND"))
                    1 -> {
                        val player = it.results.single()
                        model.addRow(listOf(it.playerName, player.name, player.country, player.rankings[category]?.points ?: 0, player.rankings[category]?.rank, "OK"))
                    }
                    else -> model.addRow(listOf(it.playerName, null, null, null, null, "MULTIPLE_MATCHES"))
                }
            }
            model
        } else {
            val model = emptyCsvListModelDoubles()
            listMatcher.matchTeam(file, charset, category).forEach { pwr ->
                val list = mutableListOf<Any?>(pwr.player1.playerName, pwr.player2.playerName)
                val p1 = addPlayerToRow(list, category, pwr.player1)
                val p2 = addPlayerToRow(list, category, pwr.player2)
                val p1Points = p1?.pointsFor(category) ?: 0
                val p2Points = p2?.pointsFor(category) ?: 0
                list.add(1, p1?.name)
                list.add(3, p2?.name)
                list.add(4, p1Points + p2Points)
                model.addRow(list)
            }
            model
        }
        csvListTable.rowSorter = null
        csvListTable.model = model
        tabbedPane.selectedIndex = 1
    }

    private fun addPlayerToRow(list: MutableList<Any?>, category: Category, playerNameWithResults: PlayerNameWithResults): ITSFPlayer? {
        return when (playerNameWithResults.results.size) {
            0 -> {
                list.addAll(listOf(null, null, null, "NOT_FOUND"))
                null
            }
            1 -> {
                val player = playerNameWithResults.results.single()
                list.addAll(listOf(player.country, player.rankings[category]?.points ?: 0, player.rankings[category]?.rank, "OK"))
                player
            }
            else -> {
                list.addAll(listOf(null, null, null, "MULTIPLE_MATCHES"))
                null
            }
        }
    }

    private fun addPlayerToSearchResultModel(model: ResultTableModel, it: ITSFPlayer) {
        val ranks = Categories.all.map { c ->
            val r = it.rankings[c]
            if (r == null)
                ""
            else
                "${r.points} (rank ${r.rank})"
        }
        val all = listOf(it.licenseNumber, it.name, it.country) + ranks
        model.addRow(all)
    }


    private fun createMainPanel(frame: JFrame): JPanel {
        jFrame = frame
        val panel = JPanel(MigLayout("wrap 2"))

        panel.add(JLabel("Load ITSF Rankings"))
        panel.add(LoadITSFDataPanel { loadITSFDATA(it) }, "growx")

        val searchAction: Action = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                searchForName()
            }
        }

        val searchLicenseAction: Action = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                searchForLicenseNumber()
            }
        }

        panel.add(JLabel("Show rankings"))
        panel.add(RankingButtonPanel { showRanking(it) }, "growx")

        panel.add(JLabel("Search player name"))
        panel.add(playerNameField.apply { addActionListener(searchAction) }, "growx")

        panel.add(JPanel())
        panel.add(JButton("Search").apply { addActionListener(searchAction) }, "growx")

        panel.add(JLabel("Search license no."))
        panel.add(itsfNoField.apply { addActionListener(searchLicenseAction) }, "growx")

        panel.add(JPanel())
        panel.add(JButton("Search").apply { addActionListener(searchLicenseAction) }, "growx")

        panel.add(JLabel("Upload player list"))
        panel.add(LoadCsvPanel(::loadCsvFile, ::checkITSFDataLoaded), "growx")

        panel.add(JLabel("Data"))

        tabbedPane.addTab("Search results", JScrollPane(searchResultTable))
        tabbedPane.addTab("Player list", JScrollPane(csvListTable))
        tabbedPane.addTab("Rankings", JScrollPane(rankingTable))
        panel.add(tabbedPane, "growx, height :400:")

        return panel
    }

    @JvmStatic
    fun main(args: Array<String>) {
        SwingUtilities.invokeLater {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            val frame = JFrame("ITSF Rankings ($version)")
            frame.contentPane.add(createMainPanel(frame))
            frame.pack()
            frame.isResizable = false
            frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            frame.isVisible = true
        }
    }

    private fun getVersionFromJarFile(): String {
        val c = ITSFRankingApp.javaClass
        val input = c.getResource('/' + c.getName().replace('.', '/') + ".class")
        return "itsf-ranking-.*all\\.jar".toRegex().find(input!!.toString())?.groupValues?.first()?.split("-")?.get(2) ?: "DEV"
    }
}