package com.burhannexus.dukaandost.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.burhannexus.dukaandost.ui.business.BusinessProfileScreen
import com.burhannexus.dukaandost.ui.common.ModuleNotBuiltScreen
import com.burhannexus.dukaandost.ui.dashboard.DashboardScreen
import com.burhannexus.dukaandost.ui.products.ProductDetailScreen
import com.burhannexus.dukaandost.ui.products.ProductEditScreen
import com.burhannexus.dukaandost.ui.products.ProductsScreen
import com.burhannexus.dukaandost.domain.model.InvoiceKind
import com.burhannexus.dukaandost.domain.model.PartyKind
import com.burhannexus.dukaandost.ui.parties.LedgerHomeScreen
import com.burhannexus.dukaandost.ui.parties.LedgerScreen
import com.burhannexus.dukaandost.ui.parties.PartiesScreen
import com.burhannexus.dukaandost.ui.returns.ReturnsScreen
import com.burhannexus.dukaandost.ui.settings.SettingsScreen
import com.burhannexus.dukaandost.ui.trade.PosScreen
import com.burhannexus.dukaandost.ui.trade.PurchaseEditorScreen
import com.burhannexus.dukaandost.ui.trade.PurchaseInvoiceScreen
import com.burhannexus.dukaandost.ui.trade.PurchasesScreen
import com.burhannexus.dukaandost.ui.trade.ReturnScreen
import com.burhannexus.dukaandost.ui.trade.SaleInvoiceScreen
import com.burhannexus.dukaandost.ui.trade.SalesHistoryScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val productIdArg = listOf(navArgument("productId") { type = NavType.LongType })
    val invoiceIdArg = listOf(navArgument("invoiceId") { type = NavType.LongType })
    val builtDestinations = setOf(
        AppDestination.Dashboard,
        AppDestination.Products,
        AppDestination.Purchases,
        AppDestination.Sales,
        AppDestination.Returns,
        AppDestination.Customers,
        AppDestination.Suppliers,
        AppDestination.Ledger,
        AppDestination.Settings
    )
    val openLedger: (PartyKind, Long) -> Unit = { kind, id ->
        navController.navigate(TradeRoutes.ledgerDetail(kind == PartyKind.CUSTOMER, id))
    }

    NavHost(
        navController = navController,
        startDestination = AppDestination.Dashboard.route,
        modifier = modifier
    ) {
        composable(AppDestination.Dashboard.route) { DashboardScreen(contentPadding) }

        composable(AppDestination.Products.route) {
            ProductsScreen(
                contentPadding = contentPadding,
                onAddProduct = { navController.navigate(ProductRoutes.edit()) },
                onOpenProduct = { id -> navController.navigate(ProductRoutes.detail(id)) }
            )
        }
        composable(ProductRoutes.EDIT, arguments = productIdArg) {
            ProductEditScreen(
                contentPadding = contentPadding,
                onClose = { navController.popBackStack() }
            )
        }
        composable(ProductRoutes.DETAIL, arguments = productIdArg) {
            ProductDetailScreen(
                contentPadding = contentPadding,
                onEdit = { id -> navController.navigate(ProductRoutes.edit(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(AppDestination.Purchases.route) {
            PurchasesScreen(
                contentPadding = contentPadding,
                onNewPurchase = { navController.navigate(TradeRoutes.PURCHASE_NEW) },
                onOpen = { id -> navController.navigate(TradeRoutes.purchaseDetail(id)) }
            )
        }
        composable(TradeRoutes.PURCHASE_NEW) {
            PurchaseEditorScreen(
                contentPadding = contentPadding,
                onDone = { navController.popBackStack() },
                onOpenInvoice = { id -> navController.navigate(TradeRoutes.purchaseDetail(id)) }
            )
        }
        composable(TradeRoutes.PURCHASE_DETAIL, arguments = invoiceIdArg) {
            PurchaseInvoiceScreen(
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onReturn = { id -> navController.navigate(TradeRoutes.purchaseReturn(id)) }
            )
        }

        composable(AppDestination.Sales.route) {
            PosScreen(
                contentPadding = contentPadding,
                onOpenHistory = { navController.navigate(TradeRoutes.SALES_HISTORY) },
                onOpenInvoice = { id -> navController.navigate(TradeRoutes.saleDetail(id)) }
            )
        }
        composable(TradeRoutes.SALES_HISTORY) {
            SalesHistoryScreen(
                contentPadding = contentPadding,
                onOpen = { id -> navController.navigate(TradeRoutes.saleDetail(id)) }
            )
        }
        composable(TradeRoutes.SALE_DETAIL, arguments = invoiceIdArg) {
            SaleInvoiceScreen(
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onReturn = { id -> navController.navigate(TradeRoutes.saleReturn(id)) }
            )
        }

        composable(AppDestination.Returns.route) {
            ReturnsScreen(
                contentPadding = contentPadding,
                onNewReturn = { kind, id ->
                    navController.navigate(
                        if (kind == InvoiceKind.SALE) TradeRoutes.saleReturn(id) else TradeRoutes.purchaseReturn(id)
                    )
                },
                onOpenInvoice = { kind, id ->
                    navController.navigate(
                        if (kind == InvoiceKind.SALE) TradeRoutes.saleDetail(id) else TradeRoutes.purchaseDetail(id)
                    )
                }
            )
        }
        composable(AppDestination.Customers.route) {
            PartiesScreen(PartyKind.CUSTOMER, contentPadding, openLedger)
        }
        composable(AppDestination.Suppliers.route) {
            PartiesScreen(PartyKind.SUPPLIER, contentPadding, openLedger)
        }
        composable(AppDestination.Ledger.route) {
            LedgerHomeScreen(contentPadding, openLedger)
        }
        composable(
            TradeRoutes.LEDGER_DETAIL,
            arguments = listOf(
                navArgument("kind") { type = NavType.StringType },
                navArgument("partyId") { type = NavType.LongType }
            )
        ) {
            LedgerScreen(contentPadding = contentPadding, onBack = { navController.popBackStack() })
        }
        composable(
            TradeRoutes.RETURN,
            arguments = listOf(
                navArgument("kind") { type = NavType.StringType },
                navArgument("invoiceId") { type = NavType.LongType }
            )
        ) {
            ReturnScreen(contentPadding = contentPadding, onDone = { navController.popBackStack() })
        }

        composable(AppDestination.Settings.route) {
            SettingsScreen(
                contentPadding = contentPadding,
                onEditBusiness = { navController.navigate(SettingsRoutes.BUSINESS) }
            )
        }
        composable(SettingsRoutes.BUSINESS) {
            BusinessProfileScreen(
                contentPadding = contentPadding,
                firstRun = false,
                onSaved = { navController.popBackStack() }
            )
        }

        AppDestination.entries
            .filter { it !in builtDestinations }
            .forEach { destination ->
                composable(destination.route) { ModuleNotBuiltScreen(destination, contentPadding) }
            }
    }
}
