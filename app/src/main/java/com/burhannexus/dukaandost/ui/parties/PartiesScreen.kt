package com.burhannexus.dukaandost.ui.parties

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import com.burhannexus.dukaandost.core.Money
import com.burhannexus.dukaandost.domain.model.PartyAccount
import com.burhannexus.dukaandost.domain.model.PartyKind
import java.math.BigDecimal

/** Customers or Suppliers: list with balances, search, add and edit. Tap a party to open its ledger. */
@Composable
fun PartiesScreen(
    kind: PartyKind,
    contentPadding: PaddingValues,
    onOpenLedger: (PartyKind, Long) -> Unit,
    viewModel: PartiesViewModel = hiltViewModel()
) {
    LaunchedEffect(kind) { viewModel.start(kind) }
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PartyAccount?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; viewModel.setQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.party_search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = ""; viewModel.setQuery("") }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_search))
                            }
                        }
                    }
                )
            }
            item { OutstandingSummary(kind, accounts) }
            if (accounts.isEmpty()) {
                item {
                    Text(
                        stringResource(if (kind == PartyKind.CUSTOMER) R.string.customers_empty else R.string.suppliers_empty),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(accounts, key = { it.id }) { account ->
                    PartyAccountCard(
                        account = account,
                        kind = kind,
                        onClick = { onOpenLedger(kind, account.id) },
                        onEdit = { editing = account }
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = { adding = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = stringResource(
                    if (kind == PartyKind.CUSTOMER) R.string.customer_add else R.string.supplier_add
                )
            )
        }
    }

    if (adding) {
        PartyFormDialog(
            kind = kind,
            initial = null,
            onDismiss = { adding = false },
            onSave = { input, done -> viewModel.save(kind, input, done) }
        )
    }
    editing?.let { account ->
        PartyFormDialog(
            kind = kind,
            initial = account,
            onDismiss = { editing = null },
            onSave = { input, done -> viewModel.save(kind, input, done) }
        )
    }
}

@Composable
fun OutstandingSummary(kind: PartyKind, accounts: List<PartyAccount>) {
    val total = accounts.fold(BigDecimal.ZERO) { acc, a -> acc.add(a.balance.max(BigDecimal.ZERO)) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(if (kind == PartyKind.CUSTOMER) R.string.total_to_receive else R.string.total_to_pay),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(Money.format(total), style = MaterialTheme.typography.titleMedium)
        }
    }
}
