package com.bitkid.itsfranking.ui

import com.bitkid.itsfranking.ITSFRankingApp
import com.bitkid.itsfranking.showLoadingDialog
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Desktop
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import java.time.LocalDateTime
import javax.swing.*


@OptIn(DelicateCoroutinesApi::class)
class LoadITSFDataPanel(private val load: suspend (String) -> Unit) : JPanel(MigLayout("insets 0 0 0 0, wrap 4")) {
    init {
        val label = JLabel("Tour")
        val currentYear = LocalDateTime.now().year
        val tourField = JComboBox(listOf(currentYear, currentYear - 1).toTypedArray())
        tourField.selectedIndex = 0
        val button = JButton("Load").apply {
            addActionListener {
                val jDialog = showLoadingDialog()
                isEnabled = false
                tourField.isEnabled = false

                val job = GlobalScope.async(Dispatchers.IO) {
                    load(tourField.selectedItem!!.toString())
                }
                GlobalScope.launch(Dispatchers.Swing) {
                    try {
                        job.await()
                        background = Color.GREEN
                        jDialog.dispose()
                    } catch (e: Exception) {
                        jDialog.dispose()
                        background = Color.RED
                        isEnabled = true
                        tourField.isEnabled = true
                        JOptionPane.showMessageDialog(
                            ITSFRankingApp.jFrame,
                            "Fetching data failed! \n ${e.stackTraceToString()}",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            }
        }
        add(label)
        add(tourField)
        add(button, "growx")
        val versionLabel = JLabel("Checking for new version ..")
        add(versionLabel, "growx")
        checkForVersionUpdate(versionLabel)
    }

    private fun checkForVersionUpdate(versionLabel: JLabel) {
        GlobalScope.launch(Dispatchers.IO) {
            val ghv = getGithubLatestVersion()
            launch(Dispatchers.Swing) {
                if (ghv != null) {
                    if (ghv == ITSFRankingApp.version) {
                        versionLabel.text = "Version ${ITSFRankingApp.version} is up to date"
                    } else {
                        versionLabel.text = """<html>Update $ghv available <a href="/">https://github.com/bitkid/itsf-ranking/packages/2052592</a></html>"""
                        versionLabel.setCursor(Cursor(Cursor.HAND_CURSOR))
                        versionLabel.addMouseListener(object : MouseAdapter() {
                            override fun mouseClicked(e: MouseEvent) {
                                try {
                                    Desktop.getDesktop().browse(URI("https://github.com/bitkid/itsf-ranking/packages/2052592"))
                                } catch (_: Exception) {
                                }
                            }
                        })
                    }
                } else {
                    versionLabel.text = "Version data not available at the moment"
                }
            }
        }
    }

    private suspend fun getGithubLatestVersion(): String? {
        try {
            HttpClient(CIO).use {
                val githubPage = it.get("https://github.com/bitkid/itsf-ranking/packages/2052592").bodyAsText()
                val match = "&lt;version&gt;.*&lt;/version&gt;".toRegex().find(githubPage)!!.groupValues.first()
                return match.replace("&lt;version&gt;", "").replace("&lt;/version&gt;", "")
            }
        } catch (e: Exception) {
            return null
        }
    }
}