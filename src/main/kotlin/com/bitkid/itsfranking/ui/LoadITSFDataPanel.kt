package com.bitkid.itsfranking.ui

import com.bitkid.itsfranking.ITSFRankingApp
import com.bitkid.itsfranking.showLoadingDialog
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.miginfocom.swing.MigLayout
import java.awt.Color
import java.time.LocalDateTime
import javax.swing.*

@OptIn(DelicateCoroutinesApi::class)
class LoadITSFDataPanel(private val load: suspend (String) -> Unit) : JPanel(MigLayout("insets 0 0 0 0, wrap 3")) {
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

                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        load(tourField.selectedItem!!.toString())
                        background = Color.GREEN
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
                    } finally {
                        jDialog.dispose()
                    }
                }
            }
        }
        add(label)
        add(tourField)
        add(button, "growx")
    }
}