package com.burhannexus.dukaandost.ui.trade

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burhannexus.dukaandost.R
import com.burhannexus.dukaandost.core.InvoiceNumbers
import com.burhannexus.dukaandost.core.Money
import com.burhannexus.dukaandost.data.pdf.PdfLabels
import com.burhannexus.dukaandost.domain.calc.InvoiceTextFormatter
import com.burhannexus.dukaandost.domain.model.BusinessProfile
import com.burhannexus.dukaandost.domain.model.InvoiceDetail
import com.burhannexus.dukaandost.domain.model.InvoiceKind
import com.burhannexus.dukaandost.domain.model.InvoiceSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")

private fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormat)

// ------------------------------------------------------------------ History lists
@Composable
fun SalesHistoryScreen(
    contentPadding: PaddingValues,
    onOpen: (Long) -> Unit,
    viewModel: SalesHistoryViewModel = hiltViewModel()
) {
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    InvoiceList(
        invoices = invoices,
        emptyText = stringResource(R.string.sales_history_empty),
        partyFallback = stringResource(R.string.walk_in_customer),
        number = { InvoiceNumbers.sale(it) },
        contentPadding = contentPadding,
        onOpen = onOpen,
        fab = null
    )
}

@Composable
fun PurchasesScreen(
    contentPadding: PaddingValues,
    onNewPurchase: () -> Unit,
    onOpen: (Long) -> Unit,
    viewModel: PurchaseHistoryViewModel = hiltViewModel()
) {
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    InvoiceList(
        invoices = invoices,
        emptyText = stringResource(R.string.purchases_empty),
        partyFallback = stringResource(R.string.no_supplier),
        number = { InvoiceNumbers.purchase(it) },
        contentPadding = contentPadding,
        onOpen = onOpen,
        fab = {
            FloatingActionButton(
                onClick = onNewPurchase,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.purchase_new)) }
        }
    )
}

@Composable
private fun InvoiceList(
    invoices: List<InvoiceSummary>,
    emptyText: String,
    partyFallback: String,
    number: (Long) -> String,
    contentPadding: PaddingValues,
    onOpen: (Long) -> Unit,
    fab: (@Composable androidx.compose.foundation.layout.BoxScope.() -> Unit)?
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        if (invoices.isEmpty()) {
            Text(
                emptyText,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(invoices, key = { it.id }) { invoice ->
                    InvoiceRowCard(invoice, number(invoice.number), partyFallback, onClick = { onOpen(invoice.id) })
                }
            }
        }
        fab?.invoke(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceRowCard(invoice: InvoiceSummary, numberText: String, partyFallback: String, onClick: () -> Unit) {
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
                Text(numberText, style = MaterialTheme.typography.titleMedium)
                Text(invoice.partyName ?: partyFallback, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(
                    formatDate(invoice.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.format(invoice.total), style = MaterialTheme.typography.titleMedium)
                if (invoice.remaining.signum() > 0) {
                    Text(
                        stringResource(R.string.invoice_remaining_short, Money.format(invoice.remaining)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        stringResource(R.string.invoice_paid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Invoice detail
@Composable
fun SaleInvoiceScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onReturn: (Long) -> Unit,
    viewModel: SaleInvoiceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    InvoiceStateContent(
        state = state,
        contentPadding = contentPadding,
        onBack = onBack,
        onReturn = onReturn,
        profile = profile,
        createPdf = { invoice, labels -> viewModel.createPdf(invoice, labels) }
    )
}

@Composable
fun PurchaseInvoiceScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onReturn: (Long) -> Unit,
    viewModel: PurchaseInvoiceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    InvoiceStateContent(
        state = state,
        contentPadding = contentPadding,
        onBack = onBack,
        onReturn = onReturn,
        profile = profile,
        createPdf = { invoice, labels -> viewModel.createPdf(invoice, labels) }
    )
}

@Composable
private fun InvoiceStateContent(
    state: InvoiceState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onReturn: (Long) -> Unit,
    profile: BusinessProfile?,
    createPdf: suspend (InvoiceDetail, PdfLabels) -> File
) {
    when (state) {
        InvoiceState.Loading -> Box(
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }

        InvoiceState.Missing -> LaunchedEffect(Unit) { onBack() }
        is InvoiceState.Found -> InvoiceContent(
            invoice = state.invoice,
            contentPadding = contentPadding,
            profile = profile,
            onReturn = { onReturn(state.invoice.id) },
            createPdf = createPdf
        )
    }
}

@Composable
private fun InvoiceContent(
    invoice: InvoiceDetail,
    contentPadding: PaddingValues,
    profile: BusinessProfile?,
    onReturn: () -> Unit,
    createPdf: suspend (InvoiceDetail, PdfLabels) -> File
) {
    val isSale = invoice.kind == InvoiceKind.SALE
    val numberText = if (isSale) InvoiceNumbers.sale(invoice.number) else InvoiceNumbers.purchase(invoice.number)
    val title = stringResource(if (isSale) R.string.invoice_sale_title else R.string.invoice_purchase_title)
    val partyLabel = stringResource(if (isSale) R.string.invoice_customer else R.string.invoice_supplier)
    val partyFallback = stringResource(if (isSale) R.string.walk_in_customer else R.string.no_supplier)
    val shareTitle = stringResource(R.string.invoice_share)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val failedText = stringResource(R.string.receipt_failed)
    val labels = PdfLabels(
        title = title,
        partyLabel = partyLabel,
        partyFallback = partyFallback,
        phone = stringResource(R.string.receipt_phone),
        item = stringResource(R.string.receipt_item),
        quantity = stringResource(R.string.receipt_qty),
        price = stringResource(R.string.receipt_price),
        amount = stringResource(R.string.receipt_amount),
        subtotal = stringResource(R.string.invoice_subtotal),
        discount = stringResource(R.string.invoice_discount),
        extraExpenses = stringResource(R.string.invoice_extra),
        total = stringResource(R.string.invoice_total),
        returned = stringResource(R.string.invoice_returned),
        paid = stringResource(R.string.invoice_paid_amount),
        refunded = stringResource(R.string.invoice_refunded),
        remaining = stringResource(R.string.pay_remaining),
        method = stringResource(R.string.invoice_method),
        methodName = stringResource(invoice.paymentMethod.labelRes())
    )

    fun withPdf(action: (File) -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                action(createPdf(invoice, labels))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(context, failedText, Toast.LENGTH_SHORT).show()
            } finally {
                busy = false
            }
        }
    }

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
            Text("$title $numberText", style = MaterialTheme.typography.headlineSmall)
            Text(
                formatDate(invoice.createdAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("$partyLabel: ${invoice.partyName ?: partyFallback}", style = MaterialTheme.typography.bodyMedium)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    invoice.lines.forEachIndexed { index, line ->
                        if (index > 0) HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(line.name, style = MaterialTheme.typography.titleMedium)
                                val discountText = if (line.discount.signum() > 0) {
                                    " (-" + Money.format(line.discount) + ")"
                                } else ""
                                Text(
                                    Money.formatQuantity(line.quantity) + " x " + Money.format(line.unitPrice) + discountText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(Money.format(line.total), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TotalRow(stringResource(R.string.invoice_subtotal), Money.format(invoice.subtotal))
                    if (invoice.invoiceDiscount.signum() > 0) {
                        TotalRow(stringResource(R.string.invoice_discount), "-" + Money.format(invoice.invoiceDiscount))
                    }
                    if (invoice.extraExpenses.signum() > 0) {
                        TotalRow(stringResource(R.string.invoice_extra), Money.format(invoice.extraExpenses))
                    }
                    TotalRow(stringResource(R.string.invoice_total), Money.format(invoice.total), bold = true)
                    if (invoice.returnedValue.signum() > 0) {
                        TotalRow(stringResource(R.string.invoice_returned), "-" + Money.format(invoice.returnedValue))
                    }
                    TotalRow(stringResource(R.string.invoice_paid_amount), Money.format(invoice.paid))
                    if (invoice.refundedAmount.signum() > 0) {
                        TotalRow(stringResource(R.string.invoice_refunded), "-" + Money.format(invoice.refundedAmount))
                    }
                    if (invoice.remaining.signum() > 0) {
                        TotalRow(stringResource(R.string.pay_remaining), Money.format(invoice.remaining), bold = true)
                    }
                    TotalRow(stringResource(R.string.invoice_method), stringResource(invoice.paymentMethod.labelRes()))
                }
            }
        }
        if (invoice.returns.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.invoice_returns_title), style = MaterialTheme.typography.titleMedium)
                        invoice.returns.forEach { r ->
                            val returnNumber = if (isSale) InvoiceNumbers.saleReturn(r.number) else InvoiceNumbers.purchaseReturn(r.number)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(returnNumber, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        formatDate(r.createdAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("-" + Money.format(r.value), style = MaterialTheme.typography.bodyMedium)
                                    if (r.refund.signum() > 0) {
                                        Text(
                                            stringResource(R.string.invoice_refund_short, Money.format(r.refund)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = onReturn, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.invoice_return_items))
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { withPdf { file -> sharePdf(context, file, shareTitle) } },
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Text(stringResource(R.string.receipt_share_pdf), modifier = Modifier.padding(start = 8.dp))
                    }
                    Button(
                        onClick = { withPdf { file -> printPdf(context, file, "$title $numberText") } },
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null)
                        Text(stringResource(R.string.receipt_print), modifier = Modifier.padding(start = 8.dp))
                    }
                }
                OutlinedButton(
                    onClick = {
                        val businessLines = listOfNotNull(
                            profile?.shopName?.takeIf { it.isNotBlank() }?.let { "*$it*" },
                            profile?.address?.takeIf { it.isNotBlank() },
                            profile?.phone?.takeIf { it.isNotBlank() }?.let { "${labels.phone}: $it" }
                        )
                        val text = InvoiceTextFormatter.format(invoice, title, partyLabel, businessLines)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(send, shareTitle))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.receipt_share_text)) }
            }
        }
    }
}

@Composable
private fun TotalRow(label: String, value: String, bold: Boolean = false) {
    val style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = style)
        Text(value, style = style)
    }
}
