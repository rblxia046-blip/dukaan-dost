package com.burhannexus.dukaandost.core

/** Human-readable, sequential invoice numbers. The numeric part is unique in the database. */
object InvoiceNumbers {
    fun sale(number: Long): String = "S-" + number.toString().padStart(6, '0')
    fun purchase(number: Long): String = "P-" + number.toString().padStart(6, '0')
    fun saleReturn(number: Long): String = "SR-" + number.toString().padStart(6, '0')
    fun customerPayment(number: Long): String = "RV-" + number.toString().padStart(6, '0')
    fun supplierPayment(number: Long): String = "PV-" + number.toString().padStart(6, '0')
    fun purchaseReturn(number: Long): String = "PR-" + number.toString().padStart(6, '0')
}
