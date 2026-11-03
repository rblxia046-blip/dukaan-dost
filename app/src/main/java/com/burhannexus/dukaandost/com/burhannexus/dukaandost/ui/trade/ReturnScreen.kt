package com.burhannexus.dukaandost.ui.trade

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burhannexus.dukaandost.R
import com.burhannexus.dukaandost.core.InvoiceNumbers
import com.burhannexus.dukaandost.core.Money
import com.burhannexus.dukaandost.domain.model.PaymentMethod

@Composable
fun ReturnScreen(
    contentPadding: PaddingValues,
    onDone: () -> Unit,
    viewModel: ReturnViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val invoice = state.invoice

    if (state.loading) {
        Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (invoice == null) {
        LaunchedEffect(Unit) { onDone() }
        return
    }

    val evaluation = evaluateReturn(invoice, state.quantities)
    val numberText = if (viewModel.isSale) InvoiceNumbers.sale(invoice.number) else InvoiceNumbers.purchase(invoice.number)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                stringResource(R.string.return_title, numberText),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                stringResource(if (viewModel.isSale) R.string.return_hint_sale else R.string.return_hint_purchase),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        items(evaluation.lines, key = { it.line.itemId }) { evalLine ->
            val line = evalLine.line
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(line.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(
                            R.string.return_line_info,
                            Money.formatQuantity(line.soldQuantity),
                            Money.formatQuantity(line.returnedQuantity),
                            Money.formatQuantity(line.returnable)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (line.returnable.signum() > 0) {
                        DecimalField(
                            value = state.quantities[line.itemId].orEmpty(),
                            onValueChange = { viewModel.setQuantity(line.itemId, it) },
                            label = stringResource(R.string.return_quantity),
                            modifier = Modifier.fillMaxWidth(),
                            isError = evalLine.hasError
                        )
                        if (evalLine.quantity.signum() > 0) {
                            Text(
                                stringResource(R.string.return_value, Money.format(evalLine.value)),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        Text(
                            stringResource(R.string.return_all_done),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.return_total_value), style = MaterialTheme.typography.titleMedium)
                        Text(Money.format(evaluation.totalValue), style = MaterialTheme.typography.titleMedium)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            stringResource(
                                if (viewModel.isSale) R.string.return_refund_sale else R.string.return_refund_purchase
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(Money.format(evaluation.refund), style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        stringResource(R.string.return_refund_explain),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (evaluation.refund.signum() > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PaymentMethod.entries.forEach { option ->
                                FilterChip(
                                    selected = state.method == option,
                                    onClick = { viewModel.setMethod(option) },
                                    label = { Text(stringResource(option.labelRes())) }
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = viewModel::save,
                enabled = !state.saving && evaluation.hasSelection,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.return_save)) }
        }
    }

    state.done?.let { done ->
        val number = if (viewModel.isSale) InvoiceNumbers.saleReturn(done.number) else InvoiceNumbers.purchaseReturn(done.number)
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.return_done_title)) },
            text = {
                Text(stringResource(R.string.return_done_body, number, Money.format(done.value), Money.format(done.refund)))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissDone()
                    onDone()
                }) { Text(stringResource(R.string.ok)) }
            }
        )
    }
}
