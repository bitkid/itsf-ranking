package com.bitkid.itsfranking.ui

import java.util.*
import javax.swing.table.DefaultTableModel

open class ResultTableModel(columns: List<String>, private val editable: Boolean = false) : DefaultTableModel(Vector(columns), 0) {
    override fun isCellEditable(row: Int, column: Int): Boolean {
        return editable
    }

    override fun getColumnClass(c: Int): Class<*> {
        val javaClass = getValueAt(0, c).javaClass
        return javaClass
    }
}

class DoublesListModel(columns: List<String>) : ResultTableModel(columns, true) {
    override fun setValueAt(aValue: Any?, row: Int, column: Int) {
        super.setValueAt(aValue, row, column)
        val current = (aValue as? Int) ?: 0
        if (column == 6) {
            super.setValueAt(((getValueAt(row, 10) as? Int) ?: 0) + current, row, 4)
        } else if (column == 10) {
            super.setValueAt(((getValueAt(row, 6) as? Int) ?: 0) + current, row, 4)
        }
    }
}

fun ResultTableModel.addRow(data: List<*>) {
    addRow(data.toTypedArray())
}