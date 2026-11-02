package com.burhannexus.dukaandost.ui.navigation

object TradeRoutes {
    const val PURCHASE_NEW = "purchase_new"
    const val PURCHASE_DETAIL = "purchase_detail/{invoiceId}"
    const val SALES_HISTORY = "sales_history"
    const val SALE_DETAIL = "sale_detail/{invoiceId}"
    const val RETURN = "return/{kind}/{invoiceId}"

    fun purchaseDetail(id: Long) = "purchase_detail/$id"
    fun saleDetail(id: Long) = "sale_detail/$id"
    fun saleReturn(id: Long) = "return/sale/$id"
    fun purchaseReturn(id: Long) = "return/purchase/$id"
}
