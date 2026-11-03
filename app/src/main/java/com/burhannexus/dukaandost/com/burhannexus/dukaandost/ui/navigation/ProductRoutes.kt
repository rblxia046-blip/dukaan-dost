package com.burhannexus.dukaandost.ui.navigation

object ProductRoutes {
    const val EDIT = "product_edit/{productId}"
    const val DETAIL = "product_detail/{productId}"

    /** id 0 = new product. */
    fun edit(id: Long = 0L) = "product_edit/$id"
    fun detail(id: Long) = "product_detail/$id"
}
