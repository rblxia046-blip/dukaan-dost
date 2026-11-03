package com.burhannexus.dukaandost.ui.returns

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burhannexus.dukaandost.R
import com.burhannexus.dukaandost.core.InvoiceNumbers
import com.burhannexus.dukaandost.core.Money
import com.burhannexus.dukaandost.domain.model.InvoiceKind
import com.burhannexus.dukaandost.domain.model.InvoiceSummary
import com.burhannexus.dukaandost.domain.model.ReturnListItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")

/**
 * The Returns menu: every sales return and purchase return in one place, and a way to start
 * a new return against any invoice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnsScreen(
    contentPadding: PaddingValues,
    onNewReturn: (InvoiceKind, Long) -> Unit,
    onOpenInvoice: (InvoiceKind, Long) -> Unit,
    viewModel: ReturnsViewModel = hiltViewModel()
) {
    var kindName by rememberSaveable { mutableStateOf(InvoiceKind.SALE.name) }
    val kind = InvoiceKind.valueOf(kindName)
    LaunchedEffect(kind) { viewModel.setKind(kind) }

    val returns by viewModel.returns.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf(false) }
    val isSale = kind == InvoiceKind.SALE

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(stringResource(R.string.returns_title), style = MaterialTheme.typography.titleLarge)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = isSale,
                    onClick = { kindName = InvoiceKind.SALE.name },
                    label = { Text(stringResource(R.string.returns_sales)) }
                )
                FilterChip(
                    selected = !isSale,
                    onClick = { kindName = InvoiceKind.PURCHASE.name },
                    label = { Text(stringResource(R.string.returns_purchases)) }
                )
            }
        }
        item {
            Button(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (isSale) R.string.returns_new_sale else R.string.returns_new_purchase))
            }
        }
        if (returns.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.returns_empty),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(returns, key = { it.id }) { item ->
                ReturnRow(item, kind) { onOpenInvoice(kind, item.invoiceId) }
            }
        }
    }

    if (picking) {
        InvoicePickerDialog(
            kind = kind,
            invoices = invoices,
            onPick = { id ->
                picking = false
                onNewReturn(kind, id)
            },
            onDismiss = { picking = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReturnRow(item: ReturnListItem, kind: InvoiceKind, onClick: () -> Unit) {
    val isSale = kind == InvoiceKind.SALE
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (isSale) InvoiceNumbers.saleReturn(item.number) else InvoiceNumbers.purchaseReturn(item.number),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    stringResource(
                        R.string.returns_from_invoice,
                        if (isSale) InvoiceNumbers.sale(item.invoiceNumber) else InvoiceNumbers.purchase(item.invoiceNumber)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                item.partyName?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
                Text(
                    Instant.ofEpochMilli(item.createdAt).atZone(ZoneId.systemDefault()).format(DateFormat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.format(item.value), style = MaterialTheme.typography.titleMedium)
                if (item.refund.signum() > 0) {
                    Text(
                        stringResource(R.string.invoice_refund_short, Money.format(item.refund)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun InvoicePickerDialog(
    kind: InvoiceKind,
    invoices: List<InvoiceSummary>,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val isSale = kind == InvoiceKind.SALE
    fun numberOf(invoice: InvoiceSummary) =
        if (isSale) InvoiceNumbers.sale(invoice.number) else InvoiceNumbers.purchase(invoice.number)

    val shown = invoices.filter {
        val text = query.trim()
        text.isEmpty() || numberOf(it).contains(text, ignoreCase = true) ||
            (it.partyName ?: "").contains(text, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.returns_pick_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.returns_pick_hint)) }
                )
                Spacer(Modifier.height(8.dp))
                if (shown.isEmpty()) {
                    Text(
                        stringResource(R.string.returns_pick_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(Modifier.heightIn(max = 340.dp)) {
                        items(shown, key = { it.id }) { invoice ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(invoice.id) }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(numberOf(invoice), style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        invoice.partyName ?: stringResource(
                                            if (isSale) R.string.walk_in_customer else R.string.no_supplier
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                Text(Money.format(invoice.total), style = MaterialTheme.typography.bodyMedium)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}
