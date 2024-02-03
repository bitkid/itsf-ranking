package com.bitkid.itsfranking.ui

import java.util.*
import javax.swing.table.DefaultTableModel

class ResultTableModel(columns: List<String>, private val editable: Boolean = false) : DefaultTableModel(Vector(columns), 0) {
    override fun isCellEditable(row: Int, column: Int): Boolean {
        return editable
    }
}

fun ResultTableModel.addRow(data: List<*>) {
    addRow(data.toTypedArray())
}