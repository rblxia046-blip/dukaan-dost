package com.burhannexus.dukaandost.core

/** Human-readable, sequential invoice numbers. The numeric part is unique in the database. */
object InvoiceNumbers {
    fun sale(number: Long): String = "S-" + number.toString().padStart(6, '0')
    fun purchase(number: Long): String = "P-" + number.toString().padStart(6, '0')
}
