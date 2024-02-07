package com.bitkid.itsfranking.ui

import com.bitkid.itsfranking.Categories
import com.bitkid.itsfranking.Category
import com.bitkid.itsfranking.ITSFRankingApp
import com.bitkid.itsfranking.showLoadingDialog
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import java.io.File
import java.nio.charset.Charset
import javax.swing.*

@DelicateCoroutinesApi
class LoadCsvPanel(private val load: (File, Charset, Category) -> Unit, private val isLoaded: () -> Boolean) : JPanel(MigLayout("insets 0 0 0 0")) {

    private var currentDirectory = File(System.getProperty("user.home"))

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
                    val r = fileChooser.showOpenDialog(ITSFRankingApp.jFrame)
                    if (r == JFileChooser.APPROVE_OPTION) {
                        val dialog = showLoadingDialog()
                        val currentFile = fileChooser.selectedFile
                        currentDirectory = currentFile.parentFile
                        val job = GlobalScope.async(Dispatchers.IO) {
                            load(
                                currentFile,
                                Charset.forName(charsetSelect.selectedItem!!.toString()),
                                Categories.all.single { it.name == category.selectedItem!! }
                            )
                        }
                        GlobalScope.launch(Dispatchers.Swing) {
                            try {
                                job.await()
                            } catch (e: Exception) {
                                dialog.dispose()
                                JOptionPane.showMessageDialog(
                                    ITSFRankingApp.jFrame,
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