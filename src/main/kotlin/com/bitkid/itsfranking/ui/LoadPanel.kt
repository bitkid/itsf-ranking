package com.bitkid.itsfranking.ui

import com.bitkid.itsfranking.showLoadingDialog
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.miginfocom.swing.MigLayout
import java.awt.Color
import javax.swing.*

@OptIn(DelicateCoroutinesApi::class)
class LoadPanel(private val jFrame: JFrame, private val load: suspend (String) -> Unit) : JPanel(MigLayout("insets 0 0 0 0, wrap 3")) {
    init {
        val label = JLabel("Tour")
        val tourField = JComboBox(listOf("2023", "2024").toTypedArray())
        tourField.selectedIndex = 1
        val button = JButton("Load").apply {
            addActionListener {
                val jDialog = showLoadingDialog(jFrame)
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
        add(label)
        add(tourField)
        add(button, "growx")
    }
}