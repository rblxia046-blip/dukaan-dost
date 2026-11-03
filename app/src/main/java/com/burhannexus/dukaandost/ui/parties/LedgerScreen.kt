package com.burhannexus.dukaandost.ui.parties

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burhannexus.dukaandost.R
import com.burhannexus.dukaandost.core.Money
import com.burhannexus.dukaandost.data.pdf.StatementLabels
import com.burhannexus.dukaandost.data.pdf.StatementRow
import com.burhannexus.dukaandost.domain.model.LedgerEntry
import com.burhannexus.dukaandost.domain.model.LedgerEntryKind
import com.burhannexus.dukaandost.domain.model.PartyKind
import com.burhannexus.dukaandost.domain.model.PaymentMethod
import com.burhannexus.dukaandost.ui.trade.DecimalField
import com.burhannexus.dukaandost.ui.trade.UiText
import com.burhannexus.dukaandost.ui.trade.asString
import com.burhannexus.dukaandost.ui.trade.labelRes
import com.burhannexus.dukaandost.ui.trade.printPdf
import com.burhannexus.dukaandost.ui.trade.sharePdf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")

@Composable
private fun entryDescription(kind: PartyKind, entry: LedgerEntry): String {
    val isCustomer = kind == PartyKind.CUSTOMER
    return when (entry.kind) {
        LedgerEntryKind.OPENING -> stringResource(R.string.ledger_opening)
        LedgerEntryKind.INVOICE -> stringResource(
            if (isCustomer) R.string.ledger_sale else R.string.ledger_purchase, entry.reference
        )
        LedgerEntryKind.PAID_WITH_INVOICE -> stringResource(R.string.ledger_paid_with, entry.reference)
        LedgerEntryKind.RETURN -> stringResource(R.string.ledger_return, entry.reference)
        LedgerEntryKind.REFUND -> stringResource(R.string.ledger_refund, entry.reference)
        LedgerEntryKind.PAYMENT -> stringResource(
            if (isCustomer) R.string.ledger_payment_received else R.string.ledger_payment_made, entry.reference
        )
    }
}

@Composable
fun LedgerScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: LedgerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        LedgerState.Loading -> Box(
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }

        LedgerState.Missing -> LaunchedEffect(Unit) { onBack() }
        is LedgerState.Found -> LedgerContent(current, contentPadding, viewModel)
    }
}

@Composable
private fun LedgerContent(found: LedgerState.Found, contentPadding: PaddingValues, viewModel: LedgerViewModel) {
    val ledger = found.ledger
    val kind = ledger.kind
    val account = ledger.account
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var showPayment by remember { mutableStateOf(false) }

    // Rows and labels for the PDF are built here, in the user's language.
    val rows = ledger.entries.map { entry ->
        StatementRow(
            createdAt = entry.createdAt,
            description = entryDescription(kind, entry),
            increase = entry.increase,
            decrease = entry.decrease,
            balance = entry.balance
        )
    }
    val closing = balanceText(kind, account.balance)
    val labels = StatementLabels(
        title = stringResource(R.string.statement_title),
        partyName = account.name,
        partyPhone = account.phone,
        phoneLabel = stringResource(R.string.receipt_phone),
        dateColumn = stringResource(R.string.statement_date),
        descriptionColumn = stringResource(R.string.statement_description),
        increaseColumn = stringResource(R.string.statement_bills),
        decreaseColumn = stringResource(R.string.statement_credits),
        balanceColumn = stringResource(R.string.statement_balance),
        closingText = closing,
        printedOn = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    )
    val failedText = stringResource(R.string.receipt_failed)
    val shareTitle = stringResource(R.string.invoice_share)

    fun withPdf(action: (File) -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                action(viewModel.createStatement(rows, labels))
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
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(account.name, style = MaterialTheme.typography.headlineSmall)
                    account.phone?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    account.address?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        closing,
                        style = MaterialTheme.typography.titleLarge,
                        color = balanceColor(kind, account.balance)
                    )
                }
            }
        }
        item {
            Button(onClick = { showPayment = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (kind == PartyKind.CUSTOMER) R.string.payment_receive else R.string.payment_make))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { withPdf { file -> sharePdf(context, file, shareTitle) } },
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Text(stringResource(R.string.statement_share), modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(
                    onClick = { withPdf { file -> printPdf(context, file, "${labels.title} ${account.name}") } },
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null)
                    Text(stringResource(R.string.receipt_print), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        item {
            Text(stringResource(R.string.ledger_entries), style = MaterialTheme.typography.titleMedium)
        }
        if (ledger.entries.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.ledger_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Newest first on screen; the PDF keeps the natural order.
            itemsIndexed(ledger.entries.reversed()) { _, entry ->
                LedgerRow(kind, entry)
            }
        }
    }

    if (showPayment) {
        PaymentDialog(
            kind = kind,
            onDismiss = { showPayment = false },
            onSave = { amount, method, note, done -> viewModel.recordPayment(amount, method, note, done) }
        )
    }
}

@Composable
private fun LedgerRow(kind: PartyKind, entry: LedgerEntry) {
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
                Text(entryDescription(kind, entry), style = MaterialTheme.typography.bodyMedium)
                if (entry.kind != LedgerEntryKind.OPENING) {
                    Text(
                        Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault()).format(DateFormat),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (entry.increase.signum() > 0) {
                    Text("+" + Money.format(entry.increase), style = MaterialTheme.typography.bodyMedium)
                }
                if (entry.decrease.signum() > 0) {
                    Text(
                        "-" + Money.format(entry.decrease),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    stringResource(R.string.ledger_balance_after, Money.format(entry.balance)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PaymentDialog(
    kind: PartyKind,
    onDismiss: () -> Unit,
    onSave: (String, PaymentMethod, String, (UiText?) -> Unit) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var error by remember { mutableStateOf<UiText?>(null) }
    var saving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = {
            Text(stringResource(if (kind == PartyKind.CUSTOMER) R.string.payment_receive else R.string.payment_make))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DecimalField(
                    value = amount,
                    onValueChange = { amount = it; error = null },
                    label = stringResource(R.string.payment_amount),
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMethod.entries.forEach { option ->
                        FilterChip(
                            selected = method == option,
                            onClick = { method = option },
                            label = { Text(stringResource(option.labelRes())) }
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.payment_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it.asString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    saving = true
                    onSave(amount, method, note) { result ->
                        saving = false
                        if (result == null) onDismiss() else error = result
                    }
                }
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
