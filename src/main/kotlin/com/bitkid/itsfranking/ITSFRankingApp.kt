package com.bitkid.itsfranking

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
import java.util.*
import javax.swing.*
import javax.swing.table.DefaultTableModel


private class RankingButtonPanel(clicked: (String) -> Unit) : JPanel(MigLayout("insets 0 0 0 0, wrap ${Categories.all.size}")) {
    init {
        Categories.all.forEach { c ->
            add(JButton(c.name).apply {
                addActionListener {
                    clicked(c.targetAudience)
                }
            }, "growx")
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
private class LoadPanel(private val jFrame: JFrame, private val load: suspend (String) -> Unit) : JPanel(MigLayout("insets 0 0 0 0, wrap 2")) {
    init {
        val tourField = JTextField(4)
        tourField.text = "2023"
        val button = JButton("Load").apply {
            addActionListener {
                val jDialog = showLoadingDialog(jFrame)
                isEnabled = false
                tourField.isEnabled = false

                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        load(tourField.text)
                        background = Color.GREEN
                    } catch (e: Exception) {
                        jDialog.dispose()
                        background = Color.RED
                        isEnabled = true
                        tourField.isEnabled = true
                        JOptionPane.showMessageDialog(
                            jFrame,
                            "Fetching data failed! \n ${e.stackTraceToString()}",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    } finally {
                        jDialog.dispose()
                    }
                }
            }
        }
        add(tourField)
        add(button, "growx")
    }
}

@OptIn(DelicateCoroutinesApi::class)
private fun showLoadingDialog(jFrame: JFrame): JDialog {
    val jDialog = JDialog(jFrame, "loading", true)
    jDialog.contentPane.layout = MigLayout()
    jDialog.isUndecorated = true
    jDialog.contentPane.add(JLabel(" LOADING ...").apply {
        border = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.black)
    }, "width :100:")
    jDialog.defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
    jDialog.pack()
    jDialog.setLocationRelativeTo(jFrame)
    GlobalScope.launch(Dispatchers.Swing) {
        jDialog.isVisible = true
    }
    return jDialog
}

@DelicateCoroutinesApi
private class LoadCsvPanel(private val jFrame: JFrame, private val load: (File, Charset, Category) -> Unit, private val isLoaded: () -> Boolean) : JPanel(MigLayout("insets 0 0 0 0")) {

    private var currentDirectory = File(".")

    init {
        val charsetSelect = JComboBox(listOf("UTF-8", "ISO-8859-1", "WINDOWS-1252").toTypedArray())
        charsetSelect.selectedIndex = 2

        val category = JComboBox(Categories.all.map { it.name }.toTypedArray())
        category.selectedIndex = 0

        val button = JButton("Open File").apply {
            addActionListener {
                if (isLoaded()) {
                    val fileChooser = JFileChooser(currentDirectory)
                    fileChooser.isMultiSelectionEnabled = false
                    fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                    val r = fileChooser.showOpenDialog(jFrame)
                    if (r == JFileChooser.APPROVE_OPTION) {
                        val dialog = showLoadingDialog(jFrame)
                        currentDirectory = fileChooser.selectedFile.parentFile
                        GlobalScope.launch(Dispatchers.IO) {
                            try {
                                val file = fileChooser.selectedFile
                                load(file, Charset.forName(charsetSelect.selectedItem!!.toString()), Categories.all.single { it.name == category.selectedItem!! })
                            } catch (e: Exception) {
                                dialog.dispose()
                                JOptionPane.showMessageDialog(
                                    jFrame,
                                    "Loading CSV list failed! \n ${e.stackTraceToString()}",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE
                                )
                            } finally {
                                dialog.dispose()
                            }
                        }
                    }
                }
            }
        }
        add(JLabel("Category:"))
        add(category, "growx")
        add(JLabel("Charset:"))
        add(charsetSelect, "growx")
        add(button, "growx")
    }
}

private class ResultTableModel(columns: List<String>, private val editable: Boolean = false) : DefaultTableModel(Vector(columns), 0) {
    override fun isCellEditable(row: Int, column: Int): Boolean {
        return editable
    }
}

@DelicateCoroutinesApi
object ITSFRankingApp {
    private lateinit var itsfPlayers: ITSFPlayers
    private lateinit var jFrame: JFrame


    private val playerNameField = JTextField(50)
    private val itsfNoField = JTextField(50)

    private val jTable = JTable(emptyModel()).apply {
        cellSelectionEnabled = true
    }

    private val jTableList = JTable(emptyListResultModelSingles()).apply {
        cellSelectionEnabled = true
    }

    private val jTableRanking = JTable(emptyRankingModel()).apply {
        cellSelectionEnabled = true
    }

    private val tabbedPane = JTabbedPane()


    private fun emptyModel() = ResultTableModel(listOf("itsf no.", "name") + Categories.all.map { it.targetAudience })

    private fun emptyRankingModel() = ResultTableModel(listOf("itsf no.", "name", "country", "rank", "points"))

    private fun emptyListResultModelSingles() = ResultTableModel(listOf("player", "itsf name", "country", "points", "status"), true)
    private fun emptyListResultModelDoubles() =
        ResultTableModel(listOf("player1", "itsf name 1", "player2", "itsf name 2", "p1 country", "p1 points", "p1 status", "p2 country", "p2 points", "p2 status"), true)

    private fun showRanking(category: String) {
        if (checkRankingLoaded()) {
            val cat = Categories.all.single { it.targetAudience == category }
            val players = itsfPlayers.getSortedRanking(cat)
            val m = emptyRankingModel()
            players.forEach {
                val itsfRank = it.rankings.getValue(cat)
                val props = listOf(it.licenseNumber, it.name, it.country, itsfRank.rank.toString(), itsfRank.points.toString())
                m.addRow(props)
            }
            jTableRanking.model = m
            tabbedPane.selectedIndex = 2
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

    private suspend fun loadRanking(s: String) {
        //val rankings = ITSFPlayerDatabaseReader(topXPlayers = 2000).readTestRankings()
        val rankings = ITSFPlayerDatabaseReader(topXPlayers = 2000, tour = s).readRankings()
        itsfPlayers = ITSFPlayers(rankings)
    }

    private fun searchLicenseNumber() {
        if (checkRankingLoaded()) {
            val text = itsfNoField.text
            if (!text.isNullOrBlank()) {
                val player = itsfPlayers.getPlayer(text)
                if (player == null)
                    jTable.model = emptyModel()
                else
                    jTable.model = modelWithPlayers(listOf(player))
                tabbedPane.selectedIndex = 0
            }
        }
    }

    private fun searchByName() {
        if (checkRankingLoaded()) {
            val text = playerNameField.text
            if (!text.isNullOrBlank()) {
                val player = itsfPlayers.find(text, true)
                jTable.model = modelWithPlayers(player)
                tabbedPane.selectedIndex = 0
            }
        }
    }


    private fun loadFile(file: File, charset: Charset, category: Category) {
        val listMatcher = ListMatcher(itsfPlayers)
        val model = if (category.type == CompetitionType.SINGLES) {
            val model = emptyListResultModelSingles()
            listMatcher.matchPlayer(file, charset, category).forEach {
                when (it.results.size) {
                    0 -> model.addRow(listOf(it.playerName, null, null, null, "NOT_FOUND"))
                    1 -> {
                        val player = it.results.single()
                        model.addRow(listOf(it.playerName, player.name, player.country, player.rankings[category]?.points?.toString() ?: "0", "OK"))
                    }

                    else -> model.addRow(listOf(it.playerName, null, null, null, "MULTIPLE_MATCHES"))
                }
            }
            model
        } else {
            val model = emptyListResultModelDoubles()
            listMatcher.matchTeam(file, charset, category).forEach { pwr ->
                val list = mutableListOf<String?>(pwr.player1.playerName, pwr.player2.playerName)
                val p1 = addPlayerToRow(list, category, pwr.player1)
                val p2 = addPlayerToRow(list, category, pwr.player2)
                list.add(1, p1?.name)
                list.add(3, p2?.name)
                model.addRow(list)
            }
            model
        }
        jTableList.model = model
        tabbedPane.selectedIndex = 1
    }

    private fun addPlayerToRow(list: MutableList<String?>, category: Category, playerNameWithResults: PlayerNameWithResults): ITSFPlayer? {
        return when (playerNameWithResults.results.size) {
            0 -> {
                list.addAll(listOf(null, null, "NOT_FOUND"))
                null
            }
            1 -> {
                val player = playerNameWithResults.results.single()
                list.addAll(listOf(player.country, player.rankings[category]?.points?.toString() ?: "0", "OK"))
                player
            }

            else -> {
                list.addAll(listOf(null, null, "MULTIPLE_MATCHES"))
                null
            }
        }
    }

    private fun addPlayerToModel(model: ResultTableModel, it: ITSFPlayer) {
        val ranks = Categories.all.map { c ->
            val r = it.rankings[c]
            if (r == null)
                ""
            else
                "${r.rank} (${r.points})"
        }
        val all = listOf(it.licenseNumber, it.name) + ranks
        model.addRow(all)
    }


    private fun createPanel(frame: JFrame): JPanel {
        jFrame = frame
        val panel = JPanel(MigLayout("wrap 2"))

        panel.add(JLabel("Load ITSF Rankings"))
        panel.add(LoadPanel(jFrame) { loadRanking(it) }, "growx")

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

        panel.add(JLabel("Upload player list"))
        panel.add(LoadCsvPanel(jFrame, ::loadFile, ::checkRankingLoaded), "growx")

        panel.add(JLabel("Results"))

        tabbedPane.addTab("Search results", JScrollPane(jTable))
        tabbedPane.addTab("Player list", JScrollPane(jTableList))
        tabbedPane.addTab("Rankings", JScrollPane(jTableRanking))
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
            val frame = JFrame("ITSF Rankings")
            frame.contentPane.add(createPanel(frame))
            frame.pack()
            frame.isResizable = false
            frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            frame.isVisible = true
        }
    }

    private fun ResultTableModel.addRow(data: List<*>) {
        addRow(data.toTypedArray())
    }
}