package com.bitkid.itsfranking.ui

import com.bitkid.itsfranking.Categories
import net.miginfocom.swing.MigLayout
import javax.swing.JButton
import javax.swing.JPanel

class RankingButtonPanel(clicked: (String) -> Unit) : JPanel(MigLayout("insets 0 0 0 0, wrap ${Categories.all.size}")) {
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