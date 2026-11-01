package com.burhannexus.dukaandost.ui.products

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burhannexus.dukaandost.R
import com.burhannexus.dukaandost.core.Money
import com.burhannexus.dukaandost.domain.model.Product
import com.burhannexus.dukaandost.domain.model.StockMovement
import com.burhannexus.dukaandost.domain.model.StockMovementType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val DateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")

@StringRes
private fun StockMovementType.labelRes(): Int = when (this) {
    StockMovementType.OPENING -> R.string.movement_opening
    StockMovementType.ADJUSTMENT -> R.string.movement_adjustment
    StockMovementType.PURCHASE -> R.string.movement_purchase
    StockMovementType.SALE -> R.string.movement_sale
    StockMovementType.SALE_RETURN -> R.string.movement_sale_return
    StockMovementType.PURCHASE_RETURN -> R.string.movement_purchase_return
}

@Composable
fun ProductDetailScreen(
    contentPadding: PaddingValues,
    onEdit: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val movements by viewModel.movements.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    when (val current = state) {
        DetailState.Loading -> Box(
            Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }

        // Product archived (or not found): leave this screen once.
        DetailState.Missing -> LaunchedEffect(Unit) { onBack() }

        is DetailState.Found -> {
            val product = current.product
            DetailContent(
                product = product,
                movements = movements,
                contentPadding = contentPadding,
                onEdit = { onEdit(product.id) },
                onDelete = { confirmDelete = true }
            )
            if (confirmDelete) {
                AlertDialog(
                    onDismissRequest = { confirmDelete = false },
                    title = { Text(stringResource(R.string.delete_product_title)) },
                    text = { Text(stringResource(R.string.delete_product_body, product.name)) },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmDelete = false
                            viewModel.archive()
                        }) { Text(stringResource(R.string.delete)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
                    }
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    product: Product,
    movements: List<StockMovement>,
    contentPadding: PaddingValues,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val notSet = stringResource(R.string.not_set)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(product.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                product.categoryName ?: stringResource(R.string.category_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            InfoCard(R.string.detail_pricing) {
                InfoRow(stringResource(R.string.detail_purchase_price), Money.format(product.purchasePrice))
                InfoRow(stringResource(R.string.detail_sale_price), Money.format(product.salePrice))
                InfoRow(stringResource(R.string.detail_profit_per_unit), Money.format(product.profitPerUnit))
            }
        }
        item {
            InfoCard(R.string.detail_stock) {
                InfoRow(
                    stringResource(R.string.detail_current_stock),
                    "${Money.formatQuantity(product.stock)} ${product.unit}"
                )
                InfoRow(
                    stringResource(R.string.detail_min_stock),
                    "${Money.formatQuantity(product.minStock)} ${product.unit}"
                )
                InfoRow(stringResource(R.string.detail_stock_value), Money.format(product.stockValue))
                InfoRow(stringResource(R.string.detail_expected_profit), Money.format(product.expectedProfit))
            }
        }
        item {
            InfoCard(R.string.detail_details) {
                InfoRow(stringResource(R.string.detail_sku), product.sku ?: notSet)
                InfoRow(stringResource(R.string.detail_barcode), product.barcode ?: notSet)
                InfoRow(stringResource(R.string.detail_expiry), product.expiryDate?.format(DateFormat) ?: notSet)
                if (product.description.isNotEmpty()) {
                    InfoRow(stringResource(R.string.detail_description), product.description)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.edit))
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        item {
            Text(stringResource(R.string.detail_history), style = MaterialTheme.typography.titleMedium)
        }
        if (movements.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(movements, key = { it.id }) { movement ->
                MovementRow(movement, product.unit)
            }
        }
    }
}

@Composable
private fun InfoCard(@StringRes titleRes: Int, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MovementRow(movement: StockMovement, unit: String) {
    val sign = if (movement.quantityChange.signum() > 0) "+" else ""
    val color = if (movement.quantityChange.signum() >= 0) MaterialTheme.colorScheme.secondary
    else MaterialTheme.colorScheme.error
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(movement.type.labelRes()), style = MaterialTheme.typography.titleMedium)
                Text(
                    Instant.ofEpochMilli(movement.createdAt).atZone(ZoneId.systemDefault()).format(DateTimeFormat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$sign${Money.formatQuantity(movement.quantityChange)} $unit",
                    style = MaterialTheme.typography.titleMedium,
                    color = color
                )
                Text(
                    stringResource(R.string.history_balance, "${Money.formatQuantity(movement.balanceAfter)} $unit"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
