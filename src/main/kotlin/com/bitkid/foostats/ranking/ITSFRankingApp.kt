package com.bitkid.foostats.ranking

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import java.awt.Color
import java.awt.event.ActionEvent
import java.util.*
import javax.swing.*
import javax.swing.table.DefaultTableModel


private class RankingButtonPanel(clicked: (String) -> Unit) : JPanel(MigLayout("insets 0 0 0 0")) {
    init {
        allCategories.forEach { c ->
            add(JButton(c.name).apply {
                addActionListener {
                    clicked(c.targetAudience)
                }
            }, "growx")
        }
    }
}

private class ResultTableModel(columns: List<String>) : DefaultTableModel(Vector(columns), 0) {
    override fun isCellEditable(row: Int, column: Int): Boolean {
        return false
    }
}

@OptIn(DelicateCoroutinesApi::class)
object ITSFRankingApp {
    private lateinit var itsfPlayers: ITSFPlayers
    private lateinit var jFrame: JFrame


    private val playerNameField = JTextField(50)
    private val itsfNoField = JTextField(50)

    private val jTable = JTable(emptyModel()).apply {
        cellSelectionEnabled = true
    }


    private fun emptyModel() = ResultTableModel(listOf("itsf no.", "name") + allCategories.map { it.targetAudience })

    private fun emptyRankingModel() = ResultTableModel(listOf("itsf no.", "name", "country", "rank", "points"))

    private fun showRanking(category: String) {
        if (checkRankingLoaded()) {
            val cat = allCategories.single { it.targetAudience == category }
            val players = itsfPlayers.getRanking(cat)
            val m = emptyRankingModel()
            players.forEach {
                val itsfRank = it.rankings.getValue(cat)
                val props =
                    listOf(it.licenseNumber, it.name, it.country, itsfRank.rank.toString(), itsfRank.points.toString())
                m.addRow(props.toTypedArray())
            }
            jTable.model = m
        }
    }

    private fun checkRankingLoaded(): Boolean {
        if (!this::itsfPlayers.isInitialized) {
            JOptionPane.showMessageDialog(jFrame, "Load rankings first!", "Info", JOptionPane.INFORMATION_MESSAGE)
            return false
        }
        return true
    }

    private fun modelWithPlayers(players: Collection<ITSFPlayer>): ResultTableModel {
        val model = emptyModel()
        players.forEach { addPlayerToModel(model, it) }
        return model
    }

    private suspend fun loadRanking() {
        // val rankings = ITSFPlayerDatabaseReader(topXPlayers = 2000).readTestRankings()
        val rankings = ITSFPlayerDatabaseReader(topXPlayers = 2000).readRankings()
        itsfPlayers = ITSFPlayers(rankings)
    }

    private fun searchLicenseNumber() {
        if (checkRankingLoaded()) {
            val text = itsfNoField.text
            if (!text.isNullOrBlank()) {
                val player = itsfPlayers.players[text]
                if (player == null)
                    jTable.model = emptyModel()
                else
                    jTable.model = modelWithPlayers(listOf(player))
            }
        }
    }

    private fun searchByName() {
        if (checkRankingLoaded()) {
            val text = playerNameField.text
            if (!text.isNullOrBlank()) {
                val player = itsfPlayers.find(text)
                jTable.model = modelWithPlayers(player)
            }
        }
    }

    private fun addPlayerToModel(model: ResultTableModel, it: ITSFPlayer) {
        val ranks = allCategories.map { c ->
            val r = it.rankings[c]
            if (r == null)
                ""
            else
                "${r.rank} (${r.points})"
        }
        val all = listOf(it.licenseNumber, it.name) + ranks
        model.addRow(all.toTypedArray())
    }


    private fun createPanel(frame: JFrame): JPanel {
        this.jFrame = frame
        val panel = JPanel(MigLayout("wrap 2"))

        panel.add(JLabel("Load ITSF Rankings"))
        panel.add(JButton("Load").apply {
            addActionListener {
                isEnabled = false
                GlobalScope.launch(Dispatchers.Swing) {
                    try {
                        loadRanking()
                        background = Color.GREEN
                    } catch (e: Exception) {
                        background = Color.RED
                        isEnabled = true
                        JOptionPane.showMessageDialog(
                            jFrame,
                            "Fetching data failed! \n ${e.stackTraceToString()}",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            }
        }, "growx")

        val searchAction: Action = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                searchByName()
            }
        }

        val searchLicenseAction: Action = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                searchLicenseNumber()
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

        panel.add(JLabel("Results"))
        panel.add(JScrollPane(jTable), "growx, height :400:")

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
            val frame = JFrame("ITSF Rankings")
            frame.contentPane.add(createPanel(frame))
            frame.pack()
            frame.isResizable = false
            frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            frame.isVisible = true
        }
    }
}