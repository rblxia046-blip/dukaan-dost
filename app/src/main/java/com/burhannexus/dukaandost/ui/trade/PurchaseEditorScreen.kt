package com.burhannexus.dukaandost.ui.trade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burhannexus.dukaandost.R
import com.burhannexus.dukaandost.core.InvoiceNumbers
import com.burhannexus.dukaandost.core.Money

@Composable
fun PurchaseEditorScreen(
    contentPadding: PaddingValues,
    onDone: () -> Unit,
    onOpenInvoice: (Long) -> Unit,
    viewModel: PurchaseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text(stringResource(R.string.purchase_new), style = MaterialTheme.typography.titleLarge) }
        item {
            PartySelector(
                label = stringResource(R.string.purchase_supplier_label),
                parties = state.suppliers,
                selectedId = state.supplierId,
                onSelect = viewModel::selectSupplier,
                onAdd = viewModel::addSupplier
            )
        }
        item {
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Search, contentDescription = null)
                Text(stringResource(R.string.pos_add_product), modifier = Modifier.padding(start = 8.dp))
            }
        }
        state.message?.let { message ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        message.asString(),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        if (state.lines.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.purchase_cart_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.lines, key = { it.product.id }) { line ->
                val discountText = if (line.discount.signum() > 0) " (-" + Money.format(line.discount) + ")" else ""
                CartLineRow(
                    name = line.product.name,
                    detail = Money.format(line.unitCost) + " x " + Money.formatQuantity(line.quantity) +
                        " " + line.product.unit + discountText,
                    total = Money.format(line.lineTotal),
                    quantityText = Money.formatQuantity(line.quantity),
                    onMinus = { viewModel.decrease(line.product.id) },
                    onPlus = { viewModel.increase(line.product.id) },
                    onEdit = { editingId = line.product.id },
                    onRemove = { viewModel.remove(line.product.id) }
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.invoice_subtotal))
                        Text(Money.format(state.subtotal))
                    }
                    DecimalField(
                        value = state.invoiceDiscountText,
                        onValueChange = viewModel::setInvoiceDiscount,
                        label = stringResource(R.string.invoice_discount),
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.discountError
                    )
                    DecimalField(
                        value = state.extraExpensesText,
                        onValueChange = viewModel::setExtraExpenses,
                        label = stringResource(R.string.purchase_extra_label),
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.extraError
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.invoice_total), style = MaterialTheme.typography.titleMedium)
                        Text(Money.format(state.total), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.pay_title), style = MaterialTheme.typography.titleMedium)
                    PaymentSection(
                        mode = state.paymentMode,
                        onModeChange = viewModel::setPaymentMode,
                        paidText = state.paidText,
                        onPaidTextChange = viewModel::setPaidText,
                        paidError = state.paidError,
                        method = state.method,
                        onMethodChange = viewModel::setMethod,
                        paid = state.paid,
                        remaining = state.remaining
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDone,
                    enabled = !state.saving,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick = viewModel::save,
                    enabled = !state.saving && state.lines.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.purchase_save)) }
            }
        }
    }

    if (showPicker) {
        ProductPickerDialog(
            onPick = { product ->
                viewModel.addProduct(product)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }

    val editingLine = state.lines.firstOrNull { it.product.id == editingId }
    if (editingLine != null) {
        LineEditDialog(
            title = editingLine.product.name,
            initialQuantity = Money.formatQuantity(editingLine.quantity),
            initialPrice = editingLine.unitCost.toPlainString(),
            initialDiscount = if (editingLine.discount.signum() > 0) editingLine.discount.toPlainString() else "",
            onConfirm = { quantity, price, discount ->
                viewModel.updateLine(editingLine.product.id, quantity, price, discount)
            },
            onDismiss = { editingId = null }
        )
    }

    state.completed?.let { done ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissCompleted() },
            title = { Text(stringResource(R.string.purchase_done_title)) },
            text = { Text(stringResource(R.string.purchase_done_body, InvoiceNumbers.purchase(done.number))) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissCompleted()
                    onOpenInvoice(done.id)
                }) { Text(stringResource(R.string.view_invoice)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.dismissCompleted()
                    onDone()
                }) { Text(stringResource(R.string.purchase_back_to_list)) }
            }
        )
    }
}
