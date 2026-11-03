package com.burhannexus.dukaandost.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.ui.graphics.vector.ImageVector
import com.burhannexus.dukaandost.R

enum class AppDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    Dashboard("dashboard", R.string.nav_dashboard, Icons.Filled.Dashboard),
    Products("products", R.string.nav_products, Icons.Filled.Inventory2),
    Purchases("purchases", R.string.nav_purchases, Icons.Filled.ShoppingCart),
    Sales("sales", R.string.nav_sales, Icons.Filled.PointOfSale),
    Returns("returns", R.string.nav_returns, Icons.Filled.AssignmentReturn),
    Customers("customers", R.string.nav_customers, Icons.Filled.People),
    Suppliers("suppliers", R.string.nav_suppliers, Icons.Filled.LocalShipping),
    Ledger("ledger", R.string.nav_ledger, Icons.Filled.MenuBook),
    Expenses("expenses", R.string.nav_expenses, Icons.Filled.Payments),
    Reports("reports", R.string.nav_reports, Icons.Filled.BarChart),
    SecretCode("secret_code", R.string.nav_secret_code, Icons.Filled.VpnKey),
    Settings("settings", R.string.nav_settings, Icons.Filled.Settings)
}
